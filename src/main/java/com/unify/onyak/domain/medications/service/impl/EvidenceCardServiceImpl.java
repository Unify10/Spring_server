package com.unify.onyak.domain.medications.service.impl;

import com.unify.onyak.domain.drugs.client.DrugApiClient;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckRequest;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse.CardInteraction;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse.CardMedication;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse.Patient;
import com.unify.onyak.domain.medications.entity.AnalysisResult;
import com.unify.onyak.domain.medications.entity.Medication;
import com.unify.onyak.domain.medications.policy.KoreanDate;
import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.policy.WardAccessValidator.Access;
import com.unify.onyak.domain.medications.repository.AnalysisResultRepository;
import com.unify.onyak.domain.medications.repository.MedicationRepository;
import com.unify.onyak.domain.medications.service.EvidenceCardService;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.global.security.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 6.2 근거 카드.
 * <p>
 * DUR 판정(약별 금기·주의 + 약끼리 병용금기·성분중복)은 결정적 규칙으로 FastAPI 가 한다.
 * 이 서비스는 ① 권한 확인 ② 복용 중인 약과 환자 정보(나이·임신)를 넘기고
 * ③ 결과의 key 를 medication_id 로 되돌린 뒤 ④ 약별 스냅샷을 저장한다.
 * <p>
 * FastAPI 호출(최대 13초) 동안 DB 커넥션을 쥐지 않도록 메서드 전체에 트랜잭션을 걸지 않는다.
 * 조회는 리포지토리 단위로 끝내고, analysis_results 저장만 짧은 트랜잭션으로 묶는다.
 */
@Service
public class EvidenceCardServiceImpl implements EvidenceCardService {

    static final String EMPTY_HEADLINE = "복용 중인 약이 없습니다.";
    static final String DEFAULT_DISCLAIMER =
            "식약처 DUR 공개 데이터 기준의 참고 정보입니다. 복용 판단은 약사·의사와 상담하세요.";

    private final WardAccessValidator wardAccessValidator;
    private final MedicationRepository medicationRepository;
    private final AnalysisResultRepository analysisResultRepository;
    private final DrugApiClient drugApiClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public EvidenceCardServiceImpl(WardAccessValidator wardAccessValidator,
                                   MedicationRepository medicationRepository,
                                   AnalysisResultRepository analysisResultRepository,
                                   DrugApiClient drugApiClient,
                                   ObjectMapper objectMapper,
                                   PlatformTransactionManager transactionManager) {
        this.wardAccessValidator = wardAccessValidator;
        this.medicationRepository = medicationRepository;
        this.analysisResultRepository = analysisResultRepository;
        this.drugApiClient = drugApiClient;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public EvidenceCardResponse getEvidenceCard(CustomUserDetails user, Long wardId) {
        Ward ward = wardAccessValidator.validate(user, wardId, Access.READ);
        Patient patient = new Patient(
                KoreanDate.ageOn(ward.getBirthDate(), KoreanDate.today()),
                ward.getIsPregnant()
        );
        OffsetDateTime now = OffsetDateTime.now(KoreanDate.ZONE);

        List<Medication> medications = medicationRepository.findAllByWardWithDrug(wardId, List.of(true));
        if (medications.isEmpty()) {
            return new EvidenceCardResponse(ward.getId(), ward.getName(), now, patient,
                    EvidenceCardResponse.LEVEL_EMPTY, EMPTY_HEADLINE,
                    List.of(), List.of(), List.of(), DEFAULT_DISCLAIMER);
        }

        // 트랜잭션 밖에서 호출한다 — 응답을 기다리는 동안 커넥션을 쥐지 않는다
        CabinetCheckResponse check = drugApiClient.cabinetCheck(toRequest(medications, patient));
        Map<String, Medication> byKey = medications.stream()
                .collect(Collectors.toMap(EvidenceCardServiceImpl::keyOf, Function.identity()));

        List<CardInteraction> interactions = check.interactions().stream()
                .map(i -> new CardInteraction(i.kind(), i.severity(), toMedicationIds(i.keys(), byKey),
                        i.itemNames(), i.detail()))
                .toList();

        List<CardMedication> cardMedications = new ArrayList<>();
        List<AnalysisResult> snapshots = new ArrayList<>();
        for (CabinetCheckResponse.Medication result : check.medications()) {
            Medication medication = byKey.get(result.key());
            if (medication == null) {
                continue;   // 요청에 없던 key — 무시
            }
            List<CardInteraction> related = relatedTo(medication.getId(), interactions);
            boolean hasWarning = !result.findings().isEmpty() || !related.isEmpty();
            CardMedication card = new CardMedication(
                    medication.getId(), result.drugId(), result.itemName(),
                    result.classification(), result.confidence(),
                    result.evidences(), result.findings(), result.summary(),
                    result.lookupFailed(), hasWarning);
            cardMedications.add(card);
            snapshots.add(AnalysisResult.of(medication, hasWarning, snapshot(card, related)));
        }
        if (!snapshots.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> analysisResultRepository.saveAll(snapshots));
        }

        return new EvidenceCardResponse(
                ward.getId(), ward.getName(), now, patient,
                check.level(), check.headline(),
                cardMedications, interactions, check.warnings(),
                check.disclaimer() != null ? check.disclaimer() : DEFAULT_DISCLAIMER
        );
    }

    static String keyOf(Medication medication) {
        return String.valueOf(medication.getId());
    }

    private static CabinetCheckRequest toRequest(List<Medication> medications, Patient patient) {
        List<CabinetCheckRequest.Item> items = medications.stream()
                .map(m -> new CabinetCheckRequest.Item(
                        keyOf(m),
                        m.getDrug().getItemSeq(),
                        m.getDrug().getName(),
                        String.join("|", m.getDrug().getIngredients())))
                .toList();
        return new CabinetCheckRequest(items, patient.age(), patient.pregnant());
    }

    private static List<Long> toMedicationIds(List<String> keys, Map<String, Medication> byKey) {
        return keys.stream()
                .map(byKey::get)
                .filter(m -> m != null)
                .map(Medication::getId)
                .toList();
    }

    private static List<CardInteraction> relatedTo(Long medicationId, List<CardInteraction> interactions) {
        return interactions.stream()
                .filter(i -> i.medicationIds().contains(medicationId))
                .toList();
    }

    /** analysis_results.detail — 이 약의 결과 + 이 약이 걸린 상호작용. */
    private String snapshot(CardMedication card, List<CardInteraction> related) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("medication", card);
        detail.put("interactions", related);
        return objectMapper.writeValueAsString(detail);
    }
}
