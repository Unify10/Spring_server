package com.unify.onyak.domain.medications.support;

import com.unify.onyak.domain.drugs.entity.Drug;
import com.unify.onyak.domain.medications.entity.Medication;
import com.unify.onyak.domain.users.auth.entity.User;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.global.security.CustomUserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/** 엔티티 생성자가 막혀 있어 테스트용으로 만들어 쓰는 객체들. */
public final class MedicationFixtures {

    private MedicationFixtures() {
    }

    public static CustomUserDetails guardian(long userId) {
        return new CustomUserDetails(userId, String.valueOf(userId), null, "ROLE_GUARDIAN");
    }

    public static CustomUserDetails wardUser(long wardId) {
        return new CustomUserDetails(wardId, String.valueOf(wardId), null, "ROLE_WARD");
    }

    /** guardianId 보호자가 관리하는 피보호자. */
    public static Ward ward(long wardId, long guardianId, LocalDate birthDate, boolean pregnant, boolean pushEnabled) {
        User guardian = mock(User.class);
        lenient().when(guardian.getId()).thenReturn(guardianId);
        Ward ward = mock(Ward.class);
        lenient().when(ward.getId()).thenReturn(wardId);
        lenient().when(ward.getGuardianId()).thenReturn(guardian);
        lenient().when(ward.getName()).thenReturn("김할머니");
        lenient().when(ward.getBirthDate()).thenReturn(birthDate);
        lenient().when(ward.getIsPregnant()).thenReturn(pregnant);
        lenient().when(ward.getMedicationPushEnabled()).thenReturn(pushEnabled);
        return ward;
    }

    public static Ward ward(long wardId, long guardianId) {
        return ward(wardId, guardianId, LocalDate.of(1948, 3, 1), false, true);
    }

    public static Drug drug(long id, String itemSeq, String name, List<String> ingredients) {
        Drug drug = Drug.create(itemSeq, name, ingredients, "제약사", null, null, null);
        ReflectionTestUtils.setField(drug, "id", id);
        return drug;
    }

    public static Medication medication(long id, Ward ward, Drug drug, List<LocalTime> times) {
        Medication medication = Medication.create(ward, drug, "1정", LocalDate.of(2026, 10, 1), times, true);
        ReflectionTestUtils.setField(medication, "id", id);
        return medication;
    }
}
