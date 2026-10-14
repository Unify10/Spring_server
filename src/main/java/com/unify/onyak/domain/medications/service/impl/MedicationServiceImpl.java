package com.unify.onyak.domain.medications.service.impl;

import com.unify.onyak.domain.drugs.entity.Drug;
import com.unify.onyak.domain.drugs.service.DrugService;
import com.unify.onyak.domain.medications.dto.request.MedicationCreateRequest;
import com.unify.onyak.domain.medications.dto.request.MedicationUpdateRequest;
import com.unify.onyak.domain.medications.dto.request.ScheduleRequest;
import com.unify.onyak.domain.medications.dto.response.MedicationResponse;
import com.unify.onyak.domain.medications.dto.response.ScheduleResponse;
import com.unify.onyak.domain.medications.entity.Medication;
import com.unify.onyak.domain.medications.error.exception.DuplicateMedicationException;
import com.unify.onyak.domain.medications.error.exception.EndDateOnActiveMedicationException;
import com.unify.onyak.domain.medications.error.exception.InactiveMedicationException;
import com.unify.onyak.domain.medications.error.exception.InvalidScheduleTimeException;
import com.unify.onyak.domain.medications.error.exception.InvalidStatusFilterException;
import com.unify.onyak.domain.medications.error.exception.MedicationNotFoundException;
import com.unify.onyak.domain.medications.policy.KoreanDate;
import com.unify.onyak.domain.medications.policy.WardAccessValidator;
import com.unify.onyak.domain.medications.policy.WardAccessValidator.Access;
import com.unify.onyak.domain.medications.repository.MedicationRepository;
import com.unify.onyak.domain.medications.service.MedicationService;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.global.security.CustomUserDetails;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class MedicationServiceImpl implements MedicationService {

    private static final int MAX_SCHEDULE_TIMES = 12;

    /** onyak.sql 의 "같은 약은 복용 중 1건만" 부분 유니크 인덱스. 동시 요청 경합을 DB 가 막는다. */
    static final String ACTIVE_UNIQUE_INDEX = "uq_medications_ward_drug_active";

    private final MedicationRepository medicationRepository;
    private final DrugService drugService;
    private final WardAccessValidator wardAccessValidator;
    private final TransactionTemplate transactionTemplate;

    public MedicationServiceImpl(MedicationRepository medicationRepository, DrugService drugService,
                                 WardAccessValidator wardAccessValidator,
                                 PlatformTransactionManager transactionManager) {
        this.medicationRepository = medicationRepository;
        this.drugService = drugService;
        this.wardAccessValidator = wardAccessValidator;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ── 5.1 가상 약장 ───────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<MedicationResponse> getMedications(CustomUserDetails user, Long wardId, String status) {
        wardAccessValidator.validate(user, wardId, Access.READ);
        List<Boolean> actives = switch (status == null ? "active" : status) {
            case "active" -> List.of(true);
            case "stopped" -> List.of(false);
            case "all" -> List.of(true, false);
            default -> throw new InvalidStatusFilterException();
        };
        return medicationRepository.findAllByWardWithDrug(wardId, actives).stream()
                .map(MedicationResponse::from)
                .toList();
    }

    /**
     * 트랜잭션을 메서드 전체에 걸지 않는다. 처음 담는 약이면 FastAPI 를 부르는데(최대 13초),
     * 그동안 DB 커넥션을 쥐고 있으면 동시 요청이 몰릴 때 풀이 고갈된다.
     * 권한 확인 → (트랜잭션 밖) 약 정보 확보 → 짧은 트랜잭션으로 중복 확인·저장 순서로 한다.
     */
    @Override
    public MedicationResponse addMedication(CustomUserDetails user, Long wardId, MedicationCreateRequest request) {
        Ward ward = wardAccessValidator.validate(user, wardId, Access.WRITE);
        List<LocalTime> times = parseTimes(request.scheduleTimes());   // 네트워크 호출 전에 입력부터 거른다
        Drug drug = drugService.getOrFetch(request.itemSeq().strip());

        Medication medication = Medication.create(
                ward,
                drug,
                request.dosage().strip(),
                request.startDate() != null ? request.startDate() : KoreanDate.today(),
                times,
                request.pushEnabled() == null || request.pushEnabled()
        );
        return transactionTemplate.execute(status -> {
            if (medicationRepository.existsByWardIdAndDrugIdAndActiveTrue(wardId, drug.getId())) {
                throw new DuplicateMedicationException();
            }
            return MedicationResponse.from(saveActive(medication));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public MedicationResponse getMedication(CustomUserDetails user, Long medicationId) {
        return MedicationResponse.from(findAccessible(user, medicationId, Access.READ));
    }

    // ── 5.2 삭제 및 중단 ────────────────────────────────────────
    @Override
    @Transactional
    public MedicationResponse updateMedication(CustomUserDetails user, Long medicationId,
                                               MedicationUpdateRequest request) {
        Medication medication = findAccessible(user, medicationId, Access.WRITE);

        if (request.dosage() != null) {
            medication.changeDosage(request.dosage().strip());
        }

        if (Boolean.FALSE.equals(request.active())) {
            // 중단 (이미 중단된 약이면 종료일만 갱신)
            medication.stop(request.endDate() != null ? request.endDate() : KoreanDate.today());
        } else if (Boolean.TRUE.equals(request.active())) {
            if (!medication.isActive()) {
                boolean duplicated = medicationRepository.existsByWardIdAndDrugIdAndActiveTrueAndIdNot(
                        medication.getWard().getId(), medication.getDrug().getId(), medication.getId());
                if (duplicated) {
                    throw new DuplicateMedicationException();
                }
                medication.resume();
                flushActive();   // 확인과 재개 사이에 같은 약이 담겼으면 여기서 유니크 인덱스에 걸린다
            }
        } else if (request.endDate() != null) {
            // active 없이 종료일만 → 이미 중단된 약의 종료일 수정만 허용
            if (medication.isActive()) {
                throw new EndDateOnActiveMedicationException();
            }
            medication.stop(request.endDate());
        }
        return MedicationResponse.from(medication);
    }

    /**
     * 잘못 담은 약을 지운다. 복용을 멈춘 거라면 삭제가 아니라 중단(PATCH active=false)을 써야 한다.
     * <p>
     * onyak.sql 설계상 medication_checks(복약 체크 기록)와 analysis_results(분석 스냅샷)가
     * ON DELETE CASCADE 라서 이 약의 기록도 함께 삭제된다.
     */
    @Override
    @Transactional
    public void deleteMedication(CustomUserDetails user, Long medicationId) {
        medicationRepository.delete(findAccessible(user, medicationId, Access.WRITE));
    }

    // ── 5.3 복용 일정 / 알림 ────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(CustomUserDetails user, Long medicationId) {
        return ScheduleResponse.from(findAccessible(user, medicationId, Access.READ));
    }

    @Override
    @Transactional
    public ScheduleResponse updateSchedule(CustomUserDetails user, Long medicationId, ScheduleRequest request) {
        Medication medication = findAccessible(user, medicationId, Access.WRITE);
        if (!medication.isActive()) {
            throw new InactiveMedicationException();
        }
        medication.updateSchedule(parseTimes(request.scheduleTimes()), request.pushEnabled());
        // 실제 푸시 발송은 추후 연동
        // medicationReminderScheduler.reschedule(medication);
        return ScheduleResponse.from(medication);
    }

    // ── 공통 ────────────────────────────────────────────────────
    private Medication findAccessible(CustomUserDetails user, Long medicationId, Access access) {
        Medication medication = medicationRepository.findWithDrugAndWardById(medicationId)
                .orElseThrow(MedicationNotFoundException::new);
        wardAccessValidator.check(user, medication.getWard(), access);
        return medication;
    }

    private Medication saveActive(Medication medication) {
        try {
            return medicationRepository.saveAndFlush(medication);
        } catch (DataIntegrityViolationException e) {
            throw toDuplicate(e);
        }
    }

    private void flushActive() {
        try {
            medicationRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw toDuplicate(e);
        }
    }

    /** 복용 중 유니크 인덱스 위반만 409 로 바꾸고, 다른 제약 위반은 그대로 올린다. */
    static RuntimeException toDuplicate(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        String message = cause.getMessage();
        if (message != null && message.contains(ACTIVE_UNIQUE_INDEX)) {
            return new DuplicateMedicationException();
        }
        return e;
    }

    /** "HH:mm" 목록 → 정렬된 LocalTime. 요청 DTO 검증을 통과하지 못한 값이 와도 여기서 한 번 더 막는다. */
    static List<LocalTime> parseTimes(List<String> times) {
        if (times == null || times.isEmpty()) {
            return List.of();
        }
        if (times.size() > MAX_SCHEDULE_TIMES) {
            throw new InvalidScheduleTimeException();
        }
        try {
            List<LocalTime> parsed = times.stream()
                    .map(t -> {
                        if (t == null || !t.matches(ScheduleRequest.TIME_REGEX)) {
                            throw new InvalidScheduleTimeException();
                        }
                        return LocalTime.parse(t);
                    })
                    .sorted()
                    .toList();
            if (parsed.stream().distinct().count() != parsed.size()) {
                throw new InvalidScheduleTimeException();
            }
            return parsed;
        } catch (DateTimeParseException e) {
            throw new InvalidScheduleTimeException();
        }
    }
}
