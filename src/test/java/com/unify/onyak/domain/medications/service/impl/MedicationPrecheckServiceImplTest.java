package com.unify.onyak.domain.medications.service.impl;

import com.unify.onyak.domain.drugs.client.DrugApiClient;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckRequest;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse.Finding;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse.Interaction;
import com.unify.onyak.domain.drugs.entity.Drug;
import com.unify.onyak.domain.drugs.error.exception.DrugNotFoundException;
import com.unify.onyak.domain.drugs.service.DrugService;
import com.unify.onyak.domain.medications.dto.request.MedicationPrecheckRequest;
import com.unify.onyak.domain.medications.dto.response.MedicationPrecheckResponse;
import com.unify.onyak.domain.medications.entity.Medication;
import com.unify.onyak.domain.medications.error.exception.WardAccessDeniedException;
import com.unify.onyak.domain.medications.policy.KoreanDate;
import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.repository.MedicationRepository;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.unify.onyak.domain.medications.support.MedicationFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MedicationPrecheckServiceImplTest {

    static final long GUARDIAN_ID = 3L;
    static final long WARD_ID = 7L;

    MedicationRepository medicationRepository;
    DrugService drugService;
    DrugApiClient drugApiClient;
    MedicationPrecheckServiceImpl service;

    Ward ward;
    Drug tylenol;     // 새로 담으려는 약
    Drug partner;     // 복용 중 — 타이레놀과 병용금기
    Drug pancold;     // 복용 중 — 아세트아미노펜 중복
    Medication partnerMed;
    Medication pancoldMed;

    @BeforeEach
    void setUp() {
        WardRepository wardRepository = mock(WardRepository.class);
        medicationRepository = mock(MedicationRepository.class);
        drugService = mock(DrugService.class);
        drugApiClient = mock(DrugApiClient.class);
        service = new MedicationPrecheckServiceImpl(new WardAccessValidator(wardRepository), medicationRepository,
                drugService, drugApiClient);

        ward = ward(WARD_ID, GUARDIAN_ID, KoreanDate.today().minusYears(78), false, true);
        when(wardRepository.findById(WARD_ID)).thenReturn(Optional.of(ward));

        tylenol = drug(1L, "195700020", "타이레놀정500mg", List.of("아세트아미노펜"));
        partner = drug(2L, "200000001", "테스트병용약정", List.of("테스트성분"));
        pancold = drug(3L, "197700444", "판콜에이내복액", List.of("아세트아미노펜", "카페인무수물"));
        partnerMed = medication(15L, ward, partner, List.of());
        pancoldMed = medication(20L, ward, pancold, List.of());
        when(drugService.getOrFetch("195700020")).thenReturn(tylenol);
    }

    private static CabinetCheckResponse.Medication result(String key, String drugId, String name, List<Finding> findings) {
        return new CabinetCheckResponse.Medication(key, drugId, name, "일반의약품", 0.86,
                List.of(), findings, Map.of(), false);
    }

    private MedicationPrecheckResponse precheck() {
        return service.precheck(guardian(GUARDIAN_ID), WARD_ID, new MedicationPrecheckRequest(" 195700020 "));
    }

    @Test
    void 새_약과_기존_약을_함께_넘기고_새_약이_걸린_결과만_돌려준다() {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of(partnerMed, pancoldMed));
        Finding odsn = new Finding("odsn_atent", "노인주의", "CAUTION", "고령자 신중 투여", null, "");
        when(drugApiClient.cabinetCheck(any())).thenReturn(new CabinetCheckResponse("DANGER", "-",
                List.of(result("15", "200000001", "테스트병용약정", List.of()),
                        result("20", "197700444", "판콜에이내복액", List.of()),
                        result("new", "195700020", "타이레놀정500mg", List.of(odsn))),
                List.of(new Interaction("병용금기", "CONTRAINDICATED", List.of("new", "15"),
                                List.of("타이레놀정500mg", "테스트병용약정"), "간독성"),
                        new Interaction("성분중복", "CAUTION", List.of("new", "20"),
                                List.of("타이레놀정500mg", "판콜에이내복액"), "아세트아미노펜"),
                        // 기존 약끼리의 문제는 이번 점검과 관계없다
                        new Interaction("성분중복", "CAUTION", List.of("15", "20"),
                                List.of("테스트병용약정", "판콜에이내복액"), "무관")),
                List.of(), "참고 정보"));

        MedicationPrecheckResponse res = precheck();

        ArgumentCaptor<CabinetCheckRequest> req = ArgumentCaptor.forClass(CabinetCheckRequest.class);
        verify(drugApiClient).cabinetCheck(req.capture());
        assertThat(req.getValue().items()).extracting(CabinetCheckRequest.Item::key).containsExactly("15", "20", "new");
        assertThat(req.getValue().items().get(2).mainIngredient()).isEqualTo("아세트아미노펜");
        assertThat(req.getValue().age()).isEqualTo(78);

        assertThat(res.level()).isEqualTo("DANGER");
        assertThat(res.headline()).isEqualTo("복용 중인 테스트병용약정와(과) 함께 먹으면 안 됩니다.");
        assertThat(res.interactions()).hasSize(2);
        assertThat(res.interactions().get(0).medicationIds()).containsExactly(15L);   // 새 약은 id 가 없다
        assertThat(res.interactions().get(1).medicationIds()).containsExactly(20L);
        assertThat(res.drug().findings()).extracting(Finding::checkKey).containsExactly("odsn_atent");
        assertThat(res.alreadyTaking()).isFalse();
    }

    @Test
    void 약장이_비어도_새_약_단독_DUR은_점검한다() {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of());
        Finding pwnm = new Finding("pwnm_taboo", "임부금기", "CONTRAINDICATED", "임부 금기", null, "");
        when(drugApiClient.cabinetCheck(any())).thenReturn(new CabinetCheckResponse("DANGER", "-",
                List.of(result("new", "195700020", "타이레놀정500mg", List.of(pwnm))), List.of(), List.of(), null));

        MedicationPrecheckResponse res = precheck();

        assertThat(res.level()).isEqualTo("DANGER");
        assertThat(res.headline()).isEqualTo("이 환자에게 금기인 항목이 1건 있습니다.");
        assertThat(res.interactions()).isEmpty();
        assertThat(res.disclaimer()).isNotBlank();
    }

    @Test
    void 성분중복이나_주의만_있으면_CAUTION() {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of(pancoldMed));
        when(drugApiClient.cabinetCheck(any())).thenReturn(new CabinetCheckResponse("CAUTION", "-",
                List.of(result("20", "197700444", "판콜에이내복액", List.of()),
                        result("new", "195700020", "타이레놀정500mg", List.of())),
                List.of(new Interaction("성분중복", "CAUTION", List.of("20", "new"),
                        List.of("판콜에이내복액", "타이레놀정500mg"), "아세트아미노펜")),
                List.of(), "참고"));

        MedicationPrecheckResponse res = precheck();

        assertThat(res.level()).isEqualTo("CAUTION");
        assertThat(res.headline()).isEqualTo("주의가 필요한 항목이 1건 있습니다.");
    }

    @Test
    void 아무것도_없으면_OK() {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of(partnerMed));
        when(drugApiClient.cabinetCheck(any())).thenReturn(new CabinetCheckResponse("OK", "-",
                List.of(result("15", "200000001", "테스트병용약정", List.of()),
                        result("new", "195700020", "타이레놀정500mg", List.of())),
                List.of(), List.of(), "참고"));

        assertThat(precheck().level()).isEqualTo("OK");
    }

    @Test
    void 새_약_정보를_못_불러오면_OK가_아니라_UNKNOWN() {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of());
        when(drugApiClient.cabinetCheck(any())).thenReturn(new CabinetCheckResponse("OK", "-",
                List.of(new CabinetCheckResponse.Medication("new", "195700020", "타이레놀정500mg", null, null,
                        List.of(), List.of(), Map.of(), true)),
                List.of(), List.of("일부 약의 정보를 불러오지 못해 점검에서 빠졌습니다."), "참고"));

        MedicationPrecheckResponse res = precheck();

        assertThat(res.level()).isEqualTo("UNKNOWN");
        assertThat(res.drug().lookupFailed()).isTrue();
    }

    @Test
    void 응답에서_새_약이_빠져도_점검된_것처럼_보이지_않게_UNKNOWN() {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of());
        when(drugApiClient.cabinetCheck(any())).thenReturn(new CabinetCheckResponse("OK", "-",
                List.of(), List.of(), List.of(), "참고"));

        MedicationPrecheckResponse res = precheck();

        assertThat(res.level()).isEqualTo("UNKNOWN");
        assertThat(res.drug().drugId()).isEqualTo("195700020");
    }

    @Test
    void 이미_복용_중인_같은_약은_비교에서_빼고_경고만_붙인다() {
        Medication takingTylenol = medication(12L, ward, tylenol, List.of());
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of(takingTylenol, partnerMed));
        when(drugApiClient.cabinetCheck(any())).thenReturn(new CabinetCheckResponse("OK", "-",
                List.of(result("15", "200000001", "테스트병용약정", List.of()),
                        result("new", "195700020", "타이레놀정500mg", List.of())),
                List.of(), List.of(), "참고"));

        MedicationPrecheckResponse res = precheck();

        ArgumentCaptor<CabinetCheckRequest> req = ArgumentCaptor.forClass(CabinetCheckRequest.class);
        verify(drugApiClient).cabinetCheck(req.capture());
        // 자기 자신과 성분중복으로 걸리지 않도록 같은 약(12)은 넘기지 않는다
        assertThat(req.getValue().items()).extracting(CabinetCheckRequest.Item::key).containsExactly("15", "new");
        assertThat(res.alreadyTaking()).isTrue();
        assertThat(res.warnings()).first().isEqualTo("이미 복용 중으로 담겨 있는 약입니다.");
    }

    @Test
    void 피보호자_본인도_점검할_수_있고_다른_보호자는_403() {
        when(medicationRepository.findAllByWardWithDrug(WARD_ID, List.of(true))).thenReturn(List.of());
        when(drugApiClient.cabinetCheck(any())).thenReturn(new CabinetCheckResponse("OK", "-",
                List.of(result("new", "195700020", "타이레놀정500mg", List.of())), List.of(), List.of(), "참고"));

        assertThat(service.precheck(wardUser(WARD_ID), WARD_ID, new MedicationPrecheckRequest("195700020")).level())
                .isEqualTo("OK");
        assertThatThrownBy(() -> service.precheck(guardian(99L), WARD_ID, new MedicationPrecheckRequest("195700020")))
                .isInstanceOf(WardAccessDeniedException.class);
    }

    @Test
    void 없는_품목이면_FastAPI_교차점검을_부르지_않고_404() {
        when(drugService.getOrFetch("000")).thenThrow(new DrugNotFoundException());

        assertThatThrownBy(() -> service.precheck(guardian(GUARDIAN_ID), WARD_ID, new MedicationPrecheckRequest("000")))
                .isInstanceOf(DrugNotFoundException.class);
        verifyNoInteractions(drugApiClient);
    }
}
