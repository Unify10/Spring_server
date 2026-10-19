package com.unify.onyak.domain.drugs.client;

import com.unify.onyak.domain.drugs.client.dto.CabinetCheckRequest;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse;
import com.unify.onyak.domain.drugs.client.dto.DrugDetail;
import com.unify.onyak.domain.drugs.error.exception.DrugApiUnavailableException;
import com.unify.onyak.domain.drugs.error.exception.DrugNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Python_server(FastAPI) 호출. 의약품 원장·DUR 판정은 그쪽이 담당하고 이 서버는 DB 만 가진다.
 * <ul>
 *   <li>FastAPI 404 → DRUG_NOT_FOUND (404)</li>
 *   <li>연결 실패·타임아웃·그 외 오류 응답 → DRUG_API_UNAVAILABLE (502)</li>
 * </ul>
 */
@Component
public class DrugApiClient {

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;

    @Autowired
    public DrugApiClient(@Value("${fastapi.base-url}") String baseUrl) {
        this(RestClient.builder().requestFactory(timeoutRequestFactory()), baseUrl);
    }

    // 테스트에서 MockRestServiceServer 를 붙인 builder 를 넘기려고 열어둔다.
    DrugApiClient(RestClient.Builder builder, String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    /** GET /drugs/{itemSeq} — 약품 상세 (drugs 테이블 캐시용). */
    public DrugDetail getDrug(String itemSeq) {
        try {
            DrugDetail detail = restClient.get()
                    .uri("/drugs/{itemSeq}", itemSeq)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new DrugNotFoundException();
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new DrugApiUnavailableException();
                    })
                    .body(DrugDetail.class);
            if (detail == null) {
                throw new DrugApiUnavailableException();
            }
            return detail;
        } catch (RestClientException e) {
            throw new DrugApiUnavailableException(e);
        }
    }

    /** POST /drugs/cabinet-check — 약장 전체 DUR + 약끼리 교차 판정 (6.2 근거 카드). */
    public CabinetCheckResponse cabinetCheck(CabinetCheckRequest request) {
        try {
            CabinetCheckResponse response = restClient.post()
                    .uri("/drugs/cabinet-check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new DrugApiUnavailableException();
                    })
                    .body(CabinetCheckResponse.class);
            if (response == null) {
                throw new DrugApiUnavailableException();
            }
            return response;
        } catch (RestClientException e) {
            throw new DrugApiUnavailableException(e);
        }
    }
}
