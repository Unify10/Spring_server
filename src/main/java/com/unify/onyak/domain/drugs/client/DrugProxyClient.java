package com.unify.onyak.domain.drugs.client;

import com.unify.onyak.domain.drugs.error.exception.DrugApiUnavailableException;
import com.unify.onyak.domain.drugs.error.exception.DrugNotFoundException;
import com.unify.onyak.domain.drugs.error.exception.DrugRequestInvalidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

/**
 * 앱이 부르는 의약품 검색·바코드·OCR 을 Python_server(FastAPI)로 그대로 넘긴다.
 * 응답 스키마는 Python_server 가 가지므로 여기서는 DTO 로 옮기지 않고 JSON 을 그대로 돌려준다.
 * Spring 이 직접 쓰는 호출(약장 담기·근거 카드)은 {@link DrugApiClient} 가 맡는다.
 * <ul>
 *   <li>FastAPI 404 → DRUG_NOT_FOUND (404)</li>
 *   <li>FastAPI 400·422 → DRUG_REQUEST_INVALID (400)</li>
 *   <li>연결 실패·타임아웃·그 외 오류 응답 → DRUG_API_UNAVAILABLE (502)</li>
 * </ul>
 */
@Slf4j
@Component
public class DrugProxyClient {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    // OCR 은 CLOVA 호출에, analyze 는 그 뒤 공공데이터 조회(재시도 포함)까지 기다려서 10초로는 모자란다
    static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final RestClient restClient;

    @Autowired
    public DrugProxyClient(@Value("${fastapi.base-url}") String baseUrl) {
        this(RestClient.builder().requestFactory(timeoutRequestFactory()), baseUrl);
    }

    // 테스트에서 MockRestServiceServer 를 붙인 builder 를 넘기려고 열어둔다.
    DrugProxyClient(RestClient.Builder builder, String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    /** GET /drugs/search — 약품명·성분명 검색. */
    public JsonNode search(String q, int limit, boolean otcOnly) {
        return get(uri -> uri.path("/drugs/search")
                .queryParam("q", q)
                .queryParam("limit", limit)
                .queryParam("otc_only", otcOnly)
                .build());
    }

    /** GET /drugs/barcode/{code} — 표준코드(GTIN-13) → 품목기준코드. */
    public JsonNode barcode(String code) {
        return get(uri -> uri.path("/drugs/barcode/{code}").build(code));
    }

    /** GET /drugs/{drugId} — 상세 + 전문/일반 판정 근거. */
    public JsonNode detail(String drugId) {
        return get(uri -> uri.path("/drugs/{drugId}").build(drugId));
    }

    /** GET /drugs/{drugId}/dur — DUR 금기·주의. 나이·임신 여부를 주면 해당 없는 점검을 건너뛴다. */
    public JsonNode dur(String drugId, Integer age, Boolean pregnant) {
        return get(uri -> uri.path("/drugs/{drugId}/dur")
                .queryParamIfPresent("age", Optional.ofNullable(age))
                .queryParamIfPresent("pregnant", Optional.ofNullable(pregnant))
                .build(drugId));
    }

    /** POST /drugs/ocr — 사진에서 약품명 후보(와 바코드)까지만 뽑는다. */
    public JsonNode ocr(MultipartFile file) {
        return postImage("/drugs/ocr", imageParts(file));
    }

    /** POST /drugs/analyze — 사진 한 장으로 품목 특정·판정·DUR 까지. */
    public JsonNode analyze(MultipartFile file, Integer age, Boolean pregnant) {
        MultipartBodyBuilder parts = imageParts(file);
        if (age != null) {
            parts.part("age", age.toString());
        }
        if (pregnant != null) {
            parts.part("pregnant", pregnant.toString());
        }
        return postImage("/drugs/analyze", parts);
    }

    private static MultipartBodyBuilder imageParts(MultipartFile file) {
        MultipartBodyBuilder parts = new MultipartBodyBuilder();
        // FastAPI 는 파일 이름의 확장자로 이미지 형식을 고르므로 원래 이름을 그대로 넘긴다
        parts.part("file", file.getResource())
                .contentType(file.getContentType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM
                        : MediaType.parseMediaType(file.getContentType()));
        return parts;
    }

    private JsonNode get(Function<UriBuilder, URI> uri) {
        return send(restClient.get().uri(uri).accept(MediaType.APPLICATION_JSON).retrieve());
    }

    private JsonNode postImage(String path, MultipartBodyBuilder parts) {
        return send(restClient.post()
                .uri(path)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .body(parts.build())
                .retrieve());
    }

    private JsonNode send(RestClient.ResponseSpec spec) {
        try {
            JsonNode body = spec
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        int status = response.getStatusCode().value();
                        // FastAPI 는 {"detail": "..."} 로 이유를 준다. 앱에는 감추고 로그로만 남긴다.
                        log.warn("FastAPI {} {} → {} {}", request.getMethod(), request.getURI().getPath(), status,
                                new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
                        if (status == 404) {
                            throw new DrugNotFoundException();
                        }
                        if (status == 400 || status == 422) {
                            throw new DrugRequestInvalidException();
                        }
                        throw new DrugApiUnavailableException();
                    })
                    .body(JsonNode.class);
            if (body == null) {
                throw new DrugApiUnavailableException();
            }
            return body;
        } catch (RestClientException e) {
            throw new DrugApiUnavailableException(e);
        }
    }
}
