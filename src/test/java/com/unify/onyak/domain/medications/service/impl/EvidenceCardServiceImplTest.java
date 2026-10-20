package com.unify.onyak.domain.medications.service.impl;

import com.unify.onyak.domain.drugs.client.DrugApiClient;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckRequest;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse.Finding;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse.Interaction;
import com.unify.onyak.domain.drugs.entity.Drug;
import com.unify.onyak.domain.drugs.error.exception.DrugApiUnavailableException;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse.CardMedication;
import com.unify.onyak.domain.medications.entity.AnalysisResult;
import com.unify.onyak.domain.medications.entity.Medication;
import com.unify.onyak.domain.medications.error.exception.WardAccessDeniedException;
import com.unify.onyak.domain.medications.policy.KoreanDate;
import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.repository.AnalysisResultRepository;
import com.unify.onyak.domain.medications.repository.MedicationRepository;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.unify.onyak.domain.medications.support.MedicationFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EvidenceCardServiceImplTest {

    static final long GUARDIAN_ID = 3L;
    static final long WARD_ID = 7L;

    MedicationRepository medicationRepository;
    AnalysisResultRepository analysisResultRepository;
    DrugApiClient drugApiClient;
    PlatformTransactionManager transactionManager;
    EvidenceCardServiceImpl service;

    Ward ward;
    Medication tylenol;
    Medication partner;
    Medication vitamin;

    @BeforeEach
    void setUp() {
        WardRepository wardRepository = mock(WardRepository.class);
        medicationRepository = mock(MedicationRepository.class);
        analysisResultRepository = mock(AnalysisResultRepository.class);
        drugApiClient = mock(DrugApiClient.class);
        transactionManager = mock(PlatformTransactionManager.class);
        service = new EvidenceCardServiceImpl(new WardAccessValidator(wardRepository), medicationRepository,
                analysisResultRepository, drugApiClient, JsonMapper.builder().build(), transactionManager);

        LocalDate birth = KoreanDate.today().minusYears(78);
        ward = ward(WARD_ID, GUARDIAN_ID, java.sql.Date.valueOf(birth), false, true);
        when(wardRepository.findById(WARD_ID)).thenReturn(Optional.of(ward));

        Drug d1 = drug(1L, "195700020", "타이레놀정500mg", List.of("아세트아미노펜"));
        Drug d2 = drug(2L, "200000001", "테스트병용약정", List.of("테스트성분", "보조성분"));
        Drug d3 = drug(3L, "300000001", "비타민정", List.of());
        tylenol = medication(12L, ward, d1, List.of());
        partner = medication(15L, ward, d2, List.of());
        vitamin = medication(20L, ward, d3, List.of());
    }

    private static CabinetCheckResponse.Medication result(String key, String drugId, List<Finding> findings) {
        return new CabinetCheckResponse.Medication(key, drugId, "이름", "일반의약품", 0.86,
                List.of(), findings, Map.of(), false);
    }

    @Test
    void 복용중인_약이_없으면_FastAPI를_부르지_않고_EMPTY() {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of());

        EvidenceCardResponse card = service.getEvidenceCard(guardian(GUARDIAN_ID), WARD_ID);

        assertThat(card.level()).isEqualTo("EMPTY");
        assertThat(card.headline()).isEqualTo("복용 중인 약이 없습니다.");
        assertThat(card.patient().age()).isEqualTo(78);
        verifyNoInteractions(drugApiClient, analysisResultRepository);
    }

    @Test
    void key를_medication_id로_되돌리고_약별_스냅샷을_저장한다() {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true)))
                .thenReturn(List.of(tylenol, partner, vitamin));
        Finding caution = new Finding("odsn_atent", "노인주의", "CAUTION", "고령자 신중 투여", null, "");
        when(drugApiClient.cabinetCheck(any())).thenReturn(new CabinetCheckResponse(
                "DANGER", "함께 먹으면 안 되는 조합이 1건 있습니다.",
                List.of(result("12", "195700020", List.of(caution)),
                        result("15", "200000001", List.of()),
                        result("20", "300000001", List.of())),
                List.of(new Interaction("병용금기", "CONTRAINDICATED", List.of("12", "15"),
                        List.of("타이레놀정500mg", "테스트병용약정"), "간독성")),
                List.of(), "참고 정보"));

        EvidenceCardResponse card = service.getEvidenceCard(guardian(GUARDIAN_ID), WARD_ID);

        // 요청: 환자 정보와 약 목록을 계약대로 넘겼는지
        ArgumentCaptor<CabinetCheckRequest> request = ArgumentCaptor.forClass(CabinetCheckRequest.class);
        verify(drugApiClient).cabinetCheck(request.capture());
        assertThat(request.getValue().age()).isEqualTo(78);
        assertThat(request.getValue().pregnant()).isFalse();
        assertThat(request.getValue().items()).extracting(CabinetCheckRequest.Item::key)
                .containsExactly("12", "15", "20");
        assertThat(request.getValue().items().get(1).mainIngredient()).isEqualTo("테스트성분|보조성분");

        // 응답: key → medication_id
        assertThat(card.level()).isEqualTo("DANGER");
        assertThat(card.interactions().get(0).medicationIds()).containsExactly(12L, 15L);
        assertThat(card.medications()).extracting(CardMedication::medicationId).containsExactly(12L, 15L, 20L);

        // has_warning: findings 가 있거나(12) 상호작용에 걸린(15) 약만
        assertThat(card.medications()).extracting(CardMedication::hasWarning).containsExactly(true, true, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AnalysisResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisResultRepository).saveAll(captor.capture());
        List<AnalysisResult> saved = captor.getValue();
        assertThat(saved).hasSize(3);
        AnalysisResult first = saved.get(0);
        assertThat(first.getMedication()).isSameAs(tylenol);
        assertThat(first.getHasWarning()).isTrue();
        assertThat(first.getDetail()).contains("odsn_atent").contains("병용금기");
        assertThat(saved.get(2).getHasWarning()).isFalse();

        // 트랜잭션은 저장할 때 한 번만, FastAPI 호출이 끝난 뒤에 연다
        var order = inOrder(drugApiClient, transactionManager, analysisResultRepository);
        order.verify(drugApiClient).cabinetCheck(any());
        order.verify(transactionManager).getTransaction(any());
        order.verify(analysisResultRepository).saveAll(any());
        order.verify(transactionManager).commit(any());
        verify(transactionManager, times(1)).getTransaction(any());
    }

    @Test
    void 피보호자_본인도_근거_카드는_볼_수_있다() {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of());

        assertThat(service.getEvidenceCard(wardUser(WARD_ID), WARD_ID).level()).isEqualTo("EMPTY");
    }

    private CabinetCheckResponse dangerResponse(String tylenolFindingKey) {
        Finding finding = new Finding(tylenolFindingKey, "라벨", "CAUTION", "내용", null, "");
        return new CabinetCheckResponse("DANGER", "헤드라인",
                List.of(result("12", "195700020", List.of(finding)),
                        result("15", "200000001", List.of())),
                List.of(new Interaction("병용금기", "CONTRAINDICATED", List.of("12", "15"),
                        List.of("타이레놀정500mg", "테스트병용약정"), "간독성")),
                List.of(), "참고 정보");
    }

    @SuppressWarnings("unchecked")
    private List<AnalysisResult> firstSaved() {
        ArgumentCaptor<List<AnalysisResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(analysisResultRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void 직전_스냅샷과_같으면_다시_저장하지_않는다() {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of(tylenol, partner));
        when(drugApiClient.cabinetCheck(any())).thenReturn(dangerResponse("odsn_atent"));

        service.getEvidenceCard(guardian(GUARDIAN_ID), WARD_ID);
        List<AnalysisResult> first = firstSaved();
        assertThat(first).hasSize(2);

        // 두 번째 조회: 직전 스냅샷이 DB 에 있다고 가정 (jsonb 처럼 키 순서가 바뀌어 있어도 같은 내용)
        when(analysisResultRepository.findLatestByMedicationIds(any())).thenReturn(first);
        clearInvocations(analysisResultRepository, transactionManager);

        service.getEvidenceCard(guardian(GUARDIAN_ID), WARD_ID);

        verify(analysisResultRepository, never()).saveAll(any());
        verifyNoInteractions(transactionManager);   // 바뀐 게 없으면 쓰기 트랜잭션도 열지 않는다
    }

    @Test
    void 판정이_바뀐_약만_새로_저장한다() {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of(tylenol, partner));
        when(drugApiClient.cabinetCheck(any())).thenReturn(dangerResponse("odsn_atent"));
        service.getEvidenceCard(guardian(GUARDIAN_ID), WARD_ID);
        List<AnalysisResult> first = firstSaved();

        when(analysisResultRepository.findLatestByMedicationIds(any())).thenReturn(first);
        when(drugApiClient.cabinetCheck(any())).thenReturn(dangerResponse("efcy_dplct"));   // 타이레놀 판정만 바뀜
        clearInvocations(analysisResultRepository);

        service.getEvidenceCard(guardian(GUARDIAN_ID), WARD_ID);

        List<AnalysisResult> second = firstSaved();
        assertThat(second).extracting(AnalysisResult::getMedication).containsExactly(tylenol);
        assertThat(second.get(0).getDetail()).contains("efcy_dplct");
    }

    @Test
    void jsonb가_키_순서를_바꿔_돌려줘도_같은_내용으로_본다() throws Exception {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of(tylenol));
        when(drugApiClient.cabinetCheck(any())).thenReturn(new CabinetCheckResponse("OK", "없음",
                List.of(result("12", "195700020", List.of())), List.of(), List.of(), "참고"));
        service.getEvidenceCard(guardian(GUARDIAN_ID), WARD_ID);
        AnalysisResult saved = firstSaved().get(0);

        // PostgreSQL jsonb 처럼 키 순서를 뒤집은 문자열로 저장돼 있었다고 가정
        var mapper = JsonMapper.builder().build();
        var tree = (tools.jackson.databind.node.ObjectNode) mapper.readTree(saved.getDetail());
        var reversed = mapper.createObjectNode();
        java.util.List<String> names = new java.util.ArrayList<>(tree.propertyNames());
        java.util.Collections.reverse(names);
        names.forEach(n -> reversed.set(n, tree.get(n)));
        AnalysisResult stored = AnalysisResult.of(tylenol, saved.getHasWarning(), mapper.writeValueAsString(reversed));

        when(analysisResultRepository.findLatestByMedicationIds(any())).thenReturn(List.of(stored));
        clearInvocations(analysisResultRepository);

        service.getEvidenceCard(guardian(GUARDIAN_ID), WARD_ID);
        verify(analysisResultRepository, never()).saveAll(any());
    }

    @Test
    void FastAPI_장애는_502로_전파된다() {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of(tylenol));
        when(drugApiClient.cabinetCheck(any())).thenThrow(new DrugApiUnavailableException());

        assertThatThrownBy(() -> service.getEvidenceCard(guardian(GUARDIAN_ID), WARD_ID))
                .isInstanceOf(DrugApiUnavailableException.class);
        verifyNoInteractions(analysisResultRepository);
    }

    @Test
    void 남의_피보호자_카드는_볼_수_없다() {
        assertThatThrownBy(() -> service.getEvidenceCard(guardian(99L), WARD_ID))
                .isInstanceOf(WardAccessDeniedException.class);
        verifyNoInteractions(drugApiClient);
    }
}
