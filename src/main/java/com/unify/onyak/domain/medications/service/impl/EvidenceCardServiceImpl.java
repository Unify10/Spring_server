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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * <p>
 * 카드를 볼 때마다 행이 쌓이지 않도록, 약별로 직전 스냅샷과 내용이 같으면 저장하지 않는다.
 * 그래서 analysis_results 에는 "판정 결과가 바뀐 시점"만 남는다.
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
        Ward ward = wardAccessValidator.validate(user, wardId, Access.WARD_OR_GUARDIAN);
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
        for (CabinetCheckResponse.Medication result : withMissingAsLookupFailed(check.medications(), medications)) {
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
        saveChangedSnapshots(snapshots);

        return new EvidenceCardResponse(
                ward.getId(), ward.getName(), now, patient,
                check.level(), check.headline(),
                cardMedications, interactions, check.warnings(),
                check.disclaimer() != null ? check.disclaimer() : DEFAULT_DISCLAIMER
        );
    }

    /**
     * 요청했는데 응답에 없는 약은 조용히 빠지면 "검사된 것처럼" 보이므로, 조회 실패(lookupFailed)로 카드에 남긴다.
     * 응답에 있는 약 뒤에 요청 순서대로 붙인다.
     */
    private static List<CabinetCheckResponse.Medication> withMissingAsLookupFailed(
            List<CabinetCheckResponse.Medication> results, List<Medication> requested) {
        Set<String> answeredKeys = results.stream()
                .map(CabinetCheckResponse.Medication::key)
                .collect(Collectors.toSet());

        List<CabinetCheckResponse.Medication> merged = new ArrayList<>(results);
        for (Medication medication : requested) {
            if (!answeredKeys.contains(keyOf(medication))) {
                merged.add(new CabinetCheckResponse.Medication(
                        keyOf(medication), medication.getDrug().getItemSeq(), medication.getDrug().getName(),
                        null, null, List.of(), List.of(), Map.of(), true));
            }
        }
        return merged;
    }

    /** 직전 스냅샷과 판정 내용이 다른 약만 저장한다. 바뀐 게 없으면 쓰기 자체를 하지 않는다. */
    private void saveChangedSnapshots(List<AnalysisResult> snapshots) {
        if (snapshots.isEmpty()) {
            return;
        }
        List<Long> medicationIds = snapshots.stream().map(a -> a.getMedication().getId()).toList();
        Map<Long, AnalysisResult> latest = analysisResultRepository.findLatestByMedicationIds(medicationIds).stream()
                .collect(Collectors.toMap(a -> a.getMedication().getId(), Function.identity(), (a, b) -> a));

        List<AnalysisResult> changed = snapshots.stream()
                .filter(s -> !sameAs(latest.get(s.getMedication().getId()), s))
                .toList();
        if (!changed.isEmpty()) {
            transactionTemplate.executeWithoutResult(status -> analysisResultRepository.saveAll(changed));
        }
    }

    /**
     * jsonb 는 저장할 때 키 순서·공백을 정규화하므로 문자열이 아니라 JSON 트리로 비교한다.
     */
    private boolean sameAs(AnalysisResult previous, AnalysisResult current) {
        if (previous == null || !current.getHasWarning().equals(previous.getHasWarning())) {
            return false;
        }
        try {
            return objectMapper.readTree(previous.getDetail()).equals(objectMapper.readTree(current.getDetail()));
        } catch (JacksonException e) {
            return false;   // 비교할 수 없으면 새로 남긴다
        }
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
