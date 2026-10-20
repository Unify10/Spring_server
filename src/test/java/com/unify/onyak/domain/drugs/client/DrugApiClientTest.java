package com.unify.onyak.domain.drugs.client;

import com.unify.onyak.domain.drugs.client.dto.CabinetCheckRequest;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse;
import com.unify.onyak.domain.drugs.client.dto.DrugDetail;
import com.unify.onyak.domain.drugs.error.exception.DrugApiUnavailableException;
import com.unify.onyak.domain.drugs.error.exception.DrugNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DrugApiClientTest {

    MockRestServiceServer server;
    DrugApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DrugApiClient(builder, "http://fastapi");
    }

    @Test
    void 약품_상세를_스네이크_케이스로_받아온다() {
        server.expect(requestTo("http://fastapi/drugs/195700020"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"drug_id":"195700020","item_name":"타이레놀정500mg","entp_name":"한국얀센",
                         "main_ingredient":"아세트아미노펜","efficacy":"해열, 진통","usage":"1회 1~2정",
                         "caution":"1일 4000mg 초과 금지","classification":"일반의약품","evidences":[]}
                        """, MediaType.APPLICATION_JSON));

        DrugDetail detail = client.getDrug("195700020");

        assertThat(detail.itemName()).isEqualTo("타이레놀정500mg");
        assertThat(detail.entpName()).isEqualTo("한국얀센");
        assertThat(detail.mainIngredient()).isEqualTo("아세트아미노펜");
        assertThat(detail.usage()).isEqualTo("1회 1~2정");
        server.verify();
    }

    @Test
    void FastAPI가_404면_DRUG_NOT_FOUND() {
        server.expect(requestTo("http://fastapi/drugs/000"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getDrug("000")).isInstanceOf(DrugNotFoundException.class);
    }

    @Test
    void FastAPI가_5xx면_DRUG_API_UNAVAILABLE() {
        server.expect(requestTo("http://fastapi/drugs/195700020"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getDrug("195700020"))
                .isInstanceOf(DrugApiUnavailableException.class);
    }

    @Test
    void 연결_실패도_DRUG_API_UNAVAILABLE() {
        server.expect(requestTo("http://fastapi/drugs/195700020"))
                .andRespond(withException(new java.net.ConnectException("refused")));

        assertThatThrownBy(() -> client.getDrug("195700020"))
                .isInstanceOf(DrugApiUnavailableException.class);
    }

    @Test
    void 약장_교차점검_요청과_응답을_계약대로_주고받는다() {
        server.expect(requestTo("http://fastapi/drugs/cabinet-check"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.items[0].key").value("12"))
                .andExpect(jsonPath("$.items[0].drug_id").value("195700020"))
                .andExpect(jsonPath("$.items[0].main_ingredient").value("아세트아미노펜"))
                .andExpect(jsonPath("$.age").value(78))
                .andExpect(jsonPath("$.pregnant").value(false))
                .andRespond(withSuccess("""
                        {"level":"DANGER","headline":"함께 먹으면 안 되는 조합이 1건 있습니다.",
                         "medications":[{"key":"12","drug_id":"195700020","item_name":"타이레놀정500mg",
                           "classification":"일반의약품","confidence":0.86,
                           "evidences":[{"source":"permit_api","verdict":"일반의약품","detail":"","weight":60}],
                           "findings":[{"check_key":"efcy_dplct","check_label":"효능군중복주의","severity":"CAUTION",
                             "prohibit_content":"중복 주의","counterpart":null,"remark":""}],
                           "summary":{"CAUTION":1},"lookup_failed":false}],
                         "interactions":[{"kind":"병용금기","severity":"CONTRAINDICATED","keys":["12","15"],
                           "item_names":["타이레놀정500mg","테스트정"],"detail":"간독성"}],
                         "warnings":[],"disclaimer":"참고 정보"}
                        """, MediaType.APPLICATION_JSON));

        CabinetCheckResponse response = client.cabinetCheck(new CabinetCheckRequest(
                List.of(new CabinetCheckRequest.Item("12", "195700020", "타이레놀정500mg", "아세트아미노펜")),
                78, false));

        assertThat(response.level()).isEqualTo("DANGER");
        assertThat(response.medications().get(0).findings().get(0).checkKey()).isEqualTo("efcy_dplct");
        assertThat(response.medications().get(0).evidences().get(0).weight()).isEqualTo(60);
        assertThat(response.interactions().get(0).keys()).containsExactly("12", "15");
        server.verify();
    }

    @Test
    void 교차점검_실패는_DRUG_API_UNAVAILABLE() {
        server.expect(requestTo("http://fastapi/drugs/cabinet-check"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT));

        assertThatThrownBy(() -> client.cabinetCheck(new CabinetCheckRequest(List.of(), null, null)))
                .isInstanceOf(DrugApiUnavailableException.class);
    }
}
