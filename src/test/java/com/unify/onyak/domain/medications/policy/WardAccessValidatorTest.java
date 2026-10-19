package com.unify.onyak.domain.medications.policy;

import com.unify.onyak.domain.medications.error.exception.MedicationWriteForbiddenException;
import com.unify.onyak.domain.medications.error.exception.WardAccessDeniedException;
import com.unify.onyak.domain.medications.error.exception.WardNotFoundException;
import com.unify.onyak.domain.medications.policy.WardAccessValidator.Access;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;

import static com.unify.onyak.domain.medications.support.MedicationFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WardAccessValidatorTest {

    WardRepository wardRepository;
    WardAccessValidator validator;
    Ward ward;

    @BeforeEach
    void setUp() {
        wardRepository = mock(WardRepository.class);
        validator = new WardAccessValidator(wardRepository);
        ward = ward(7L, 3L);   // thenReturn 안에서 목을 만들면 Mockito 가 중첩 스터빙으로 막는다
        when(wardRepository.findById(7L)).thenReturn(Optional.of(ward));
    }

    @ParameterizedTest
    @EnumSource(Access.class)
    void 보호자는_자기_피보호자에_모든_권한이_있다(Access access) {
        assertThat(validator.validate(guardian(3L), 7L, access)).isSameAs(ward);
    }

    @ParameterizedTest
    @EnumSource(Access.class)
    void 다른_보호자는_모두_403(Access access) {
        assertThatThrownBy(() -> validator.validate(guardian(99L), 7L, access))
                .isInstanceOf(WardAccessDeniedException.class);
    }

    @Test
    void 피보호자_본인은_WARD_OR_GUARDIAN만_통과한다() {
        assertThat(validator.validate(wardUser(7L), 7L, Access.WARD_OR_GUARDIAN)).isSameAs(ward);
        assertThatThrownBy(() -> validator.validate(wardUser(7L), 7L, Access.GUARDIAN_ONLY))
                .isInstanceOf(MedicationWriteForbiddenException.class);
    }

    @ParameterizedTest
    @EnumSource(Access.class)
    void 다른_피보호자는_403(Access access) {
        assertThatThrownBy(() -> validator.validate(wardUser(8L), 7L, access))
                .isInstanceOf(WardAccessDeniedException.class);
    }

    @Test
    void 없는_피보호자면_404() {
        when(wardRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(guardian(3L), 9L, Access.WARD_OR_GUARDIAN))
                .isInstanceOf(WardNotFoundException.class);
    }

    @Test
    void 인증정보가_없으면_403() {
        assertThatThrownBy(() -> validator.validate(null, 7L, Access.WARD_OR_GUARDIAN))
                .isInstanceOf(WardAccessDeniedException.class);
    }
}
