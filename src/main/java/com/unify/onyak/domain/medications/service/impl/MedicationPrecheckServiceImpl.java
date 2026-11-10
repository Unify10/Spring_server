package com.unify.onyak.domain.medications.service.impl;

import com.unify.onyak.domain.drugs.client.DrugApiClient;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckRequest;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse.Finding;
import com.unify.onyak.domain.drugs.client.dto.CabinetCheckResponse.Interaction;
import com.unify.onyak.domain.drugs.entity.Drug;
import com.unify.onyak.domain.drugs.service.DrugService;
import com.unify.onyak.domain.medications.dto.request.MedicationPrecheckRequest;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse.CardInteraction;
import com.unify.onyak.domain.medications.dto.response.EvidenceCardResponse.Patient;
import com.unify.onyak.domain.medications.dto.response.MedicationPrecheckResponse;
import com.unify.onyak.domain.medications.dto.response.MedicationPrecheckResponse.PrecheckDrug;
import com.unify.onyak.domain.medications.entity.Medication;
import com.unify.onyak.domain.medications.policy.KoreanDate;
import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.policy.WardAccessValidator.Access;
import com.unify.onyak.domain.medications.repository.MedicationRepository;
import com.unify.onyak.domain.medications.service.MedicationPrecheckService;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.unify.onyak.domain.medications.dto.response.MedicationPrecheckResponse.*;

/**
 * 4.1~4.3 약 담기 전 미리 점검.
 * <p>
 * 지금 복용 중인 약 + 새 약을 FastAPI cabinet-check 에 한 번에 넘기고, 그중 <b>새 약이 걸린 결과만</b> 골라낸다.
 * 판정 규칙은 근거 카드(6.2)와 같다 — 결정적 규칙, FastAPI 가 판정한다.
 * 아무것도 저장하지 않는다 (약장 담기·스냅샷 없음). drugs 캐시만 채워질 수 있다.
 * FastAPI 호출 동안 DB 커넥션을 쥐지 않도록 트랜잭션을 걸지 않는다.
 */
@Service
@RequiredArgsConstructor
public class MedicationPrecheckServiceImpl implements MedicationPrecheckService {

    static final String NEW_KEY = "new";
    static final String CONTRAINDICATED = "CONTRAINDICATED";
    static final String CAUTION = "CAUTION";
    static final String TABOO = "병용금기";

    private final WardAccessValidator wardAccessValidator;
    private final MedicationRepository medicationRepository;
    private final DrugService drugService;
    private final DrugApiClient drugApiClient;

    @Override
    public MedicationPrecheckResponse precheck(CustomUserDetails user, Long wardId, MedicationPrecheckRequest request) {
        Ward ward = wardAccessValidator.validate(user, wardId, Access.WARD_OR_GUARDIAN);
        Patient patient = new Patient(
                KoreanDate.ageOn(ward.getBirthDate(), KoreanDate.today()),
                ward.getIsPregnant()
        );
        Drug drug = drugService.getOrFetch(request.itemSeq().strip());   // 없는 품목이면 404

        List<Medication> taking = medicationRepository.findAllByWardWithDrug(wardId, List.of(true));
        boolean alreadyTaking = taking.stream().anyMatch(m -> Objects.equals(m.getDrug().getId(), drug.getId()));
        // 같은 약끼리 비교하면 성분중복이 무조건 나오므로, 이미 복용 중인 같은 약은 비교 대상에서 뺀다
        List<Medication> others = taking.stream()
                .filter(m -> !Objects.equals(m.getDrug().getId(), drug.getId()))
                .toList();

        CabinetCheckResponse check = drugApiClient.cabinetCheck(toRequest(drug, others, patient));

        PrecheckDrug newDrug = check.medications().stream()
                .filter(m -> NEW_KEY.equals(m.key()))
                .findFirst()
                .map(m -> new PrecheckDrug(m.drugId(), m.itemName(), m.classification(), m.confidence(),
                        m.evidences(), m.findings(), m.summary(), m.lookupFailed()))
                .orElseGet(() -> new PrecheckDrug(drug.getItemSeq(), drug.getName(), null, null,
                        List.of(), List.of(), Map.of(), true));   // 응답에서 빠졌으면 점검 못 한 것으로

        Map<String, Medication> byKey = others.stream()
                .collect(Collectors.toMap(EvidenceCardServiceImpl::keyOf, Function.identity()));
        List<CardInteraction> interactions = check.interactions().stream()
                .filter(i -> i.keys().contains(NEW_KEY))
                .map(i -> toCard(i, byKey))
                .toList();

        List<String> warnings = new ArrayList<>(check.warnings());
        if (alreadyTaking) {
            warnings.add(0, "이미 복용 중으로 담겨 있는 약입니다.");
        }
        String[] grade = grade(newDrug, interactions);
        return new MedicationPrecheckResponse(
                ward.getId(), patient, grade[0], grade[1], alreadyTaking, newDrug, interactions, warnings,
                check.disclaimer() != null ? check.disclaimer() : EvidenceCardServiceImpl.DEFAULT_DISCLAIMER
        );
    }

    private static CabinetCheckRequest toRequest(Drug drug, List<Medication> others, Patient patient) {
        List<CabinetCheckRequest.Item> items = new ArrayList<>();
        for (Medication m : others) {
            items.add(new CabinetCheckRequest.Item(EvidenceCardServiceImpl.keyOf(m), m.getDrug().getItemSeq(),
                    m.getDrug().getName(), String.join("|", m.getDrug().getIngredients())));
        }
        items.add(new CabinetCheckRequest.Item(NEW_KEY, drug.getItemSeq(), drug.getName(),
                String.join("|", drug.getIngredients())));
        return new CabinetCheckRequest(items, patient.age(), patient.pregnant());
    }

    /** 새 약과 걸린 기존 약만 medication_ids 로 남긴다. */
    private static CardInteraction toCard(Interaction interaction, Map<String, Medication> byKey) {
        List<Long> medicationIds = interaction.keys().stream()
                .map(byKey::get)
                .filter(Objects::nonNull)
                .map(Medication::getId)
                .toList();
        return new CardInteraction(interaction.kind(), interaction.severity(), medicationIds,
                interaction.itemNames(), interaction.detail());
    }

    /** {level, headline} — 새 약에 관한 결과만으로 정한다. */
    static String[] grade(PrecheckDrug drug, List<CardInteraction> interactions) {
        List<String> tabooWith = interactions.stream()
                .filter(i -> TABOO.equals(i.kind()))
                .flatMap(i -> i.itemNames().stream())
                .filter(name -> !Objects.equals(name, drug.itemName()))
                .distinct()
                .toList();
        if (!tabooWith.isEmpty()) {
            return new String[]{LEVEL_DANGER, "복용 중인 " + String.join(", ", tabooWith) + "와(과) 함께 먹으면 안 됩니다."};
        }
        long danger = drug.findings().stream().map(Finding::severity).filter(CONTRAINDICATED::equals).count();
        if (danger > 0) {
            return new String[]{LEVEL_DANGER, "이 환자에게 금기인 항목이 " + danger + "건 있습니다."};
        }
        if (drug.lookupFailed()) {
            return new String[]{LEVEL_UNKNOWN, "약 정보를 불러오지 못해 점검하지 못했습니다. 잠시 후 다시 확인하세요."};
        }
        long caution = drug.findings().stream().map(Finding::severity).filter(CAUTION::equals).count()
                + interactions.size();
        if (caution > 0) {
            return new String[]{LEVEL_CAUTION, "주의가 필요한 항목이 " + caution + "건 있습니다."};
        }
        return new String[]{LEVEL_OK, "복용 중인 약과 겹치는 금기·주의 정보가 없습니다."};
    }
}
