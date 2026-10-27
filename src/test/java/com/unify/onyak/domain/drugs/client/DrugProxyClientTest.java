package com.unify.onyak.domain.drugs.client;

import com.unify.onyak.domain.drugs.error.exception.DrugApiUnavailableException;
import com.unify.onyak.domain.drugs.error.exception.DrugNotFoundException;
import com.unify.onyak.domain.drugs.error.exception.DrugRequestInvalidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DrugProxyClientTest {

    MockRestServiceServer server;
    DrugProxyClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DrugProxyClient(builder, "http://fastapi");
    }

    @Test
    void 검색은_한글_검색어를_인코딩해_넘기고_응답을_그대로_돌려준다() {
        server.expect(requestTo("http://fastapi/drugs/search?q=%ED%83%80%EC%9D%B4%EB%A0%88%EB%86%80&limit=5&otc_only=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"query":"타이레놀","total":1,"items":[{"drug_id":"195700020","item_name":"타이레놀정500mg"}]}
                        """, MediaType.APPLICATION_JSON));

        JsonNode result = client.search("타이레놀", 5, true);

        assertThat(result.get("total").asInt()).isEqualTo(1);
        assertThat(result.get("items").get(0).get("drug_id").asString()).isEqualTo("195700020");
        server.verify();
    }

    @Test
    void DUR은_나이_임신_여부를_준_것만_쿼리로_넘긴다() {
        server.expect(requestTo("http://fastapi/drugs/195700020/dur?age=78"))
                .andRespond(withSuccess("{\"findings\":[]}", MediaType.APPLICATION_JSON));

        client.dur("195700020", 78, null);

        server.verify();
    }

    @Test
    void OCR은_원래_파일_이름으로_멀티파트를_넘긴다() {
        // FastAPI 는 파일 확장자로 이미지 형식을 고른다
        server.expect(requestTo("http://fastapi/drugs/ocr"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", containsString("multipart/form-data")))
                .andExpect(content().string(allOf(
                        containsString("name=\"file\"; filename=\"pill.png\""),
                        containsString("PNGDATA"))))
                .andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

        client.ocr(new MockMultipartFile("file", "pill.png", "image/png", "PNGDATA".getBytes()));

        server.verify();
    }

    @Test
    void analyze는_나이_임신_여부를_폼_필드로_함께_넘긴다() {
        server.expect(requestTo("http://fastapi/drugs/analyze"))
                .andExpect(content().string(allOf(
                        containsString("name=\"age\""),
                        containsString("78"),
                        containsString("name=\"pregnant\""))))
                .andRespond(withSuccess("{\"drug\":null}", MediaType.APPLICATION_JSON));

        client.analyze(new MockMultipartFile("file", "pill.jpg", "image/jpeg", new byte[]{1}), 78, false);

        server.verify();
    }

    @Test
    void FastAPI가_404면_DRUG_NOT_FOUND() {
        server.expect(requestTo("http://fastapi/drugs/000"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("{\"detail\":\"없음\"}"));

        assertThatThrownBy(() -> client.detail("000")).isInstanceOf(DrugNotFoundException.class);
    }

    @Test
    void FastAPI가_400이면_DRUG_REQUEST_INVALID() {
        // 빈 파일을 올리면 FastAPI 가 400 을 준다
        server.expect(requestTo("http://fastapi/drugs/ocr"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"detail\":\"빈 파일입니다.\"}"));

        assertThatThrownBy(() -> client.ocr(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0])))
                .isInstanceOf(DrugRequestInvalidException.class);
    }

    @Test
    void FastAPI가_502면_DRUG_API_UNAVAILABLE() {
        // CLOVA·공공데이터 장애는 FastAPI 가 502 로 준다
        server.expect(requestTo("http://fastapi/drugs/ocr"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("{\"detail\":\"OCR 실패\"}"));

        assertThatThrownBy(() -> client.ocr(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1})))
                .isInstanceOf(DrugApiUnavailableException.class);
    }

    @Test
    void 연결_실패도_DRUG_API_UNAVAILABLE() {
        server.expect(requestTo("http://fastapi/drugs/barcode/8806469007213"))
                .andRespond(withException(new java.net.ConnectException("refused")));

        assertThatThrownBy(() -> client.barcode("8806469007213"))
                .isInstanceOf(DrugApiUnavailableException.class);
    }
}
