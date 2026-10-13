package com.unify.onyak.domain.medications.policy;

import com.unify.onyak.domain.medications.error.exception.WardAccessDeniedException;
import com.unify.onyak.domain.medications.error.exception.WardNotFoundException;
import com.unify.onyak.domain.wards.auth.entity.Ward;
import com.unify.onyak.domain.wards.auth.repository.WardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.unify.onyak.domain.medications.support.MedicationFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WardAccessValidatorTest {

    WardRepository wardRepository;
    WardAccessValidator validator;

    @BeforeEach
    void setUp() {
        wardRepository = mock(WardRepository.class);
        validator = new WardAccessValidator(wardRepository);
    }

    @Test
    void 자기_피보호자면_통과한다() {
        Ward ward = ward(7L, 3L);
        when(wardRepository.findById(7L)).thenReturn(Optional.of(ward));

        assertThat(validator.validate(guardian(3L), 7L)).isSameAs(ward);
    }

    @Test
    void 남의_피보호자면_403() {
        Ward ward = ward(7L, 3L);   // thenReturn 안에서 목을 만들면 Mockito 가 중첩 스터빙으로 막는다
        when(wardRepository.findById(7L)).thenReturn(Optional.of(ward));

        assertThatThrownBy(() -> validator.validate(guardian(99L), 7L))
                .isInstanceOf(WardAccessDeniedException.class);
    }

    @Test
    void 피보호자_본인은_통과하고_다른_피보호자는_403() {
        Ward ward = ward(7L, 3L);
        when(wardRepository.findById(7L)).thenReturn(Optional.of(ward));

        assertThat(validator.validate(wardUser(7L), 7L)).isSameAs(ward);
        assertThatThrownBy(() -> validator.validate(wardUser(8L), 7L))
                .isInstanceOf(WardAccessDeniedException.class);
    }

    @Test
    void 없는_피보호자면_404() {
        when(wardRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(guardian(3L), 7L))
                .isInstanceOf(WardNotFoundException.class);
    }

    @Test
    void 인증정보가_없으면_403() {
        Ward ward = ward(7L, 3L);   // thenReturn 안에서 목을 만들면 Mockito 가 중첩 스터빙으로 막는다
        when(wardRepository.findById(7L)).thenReturn(Optional.of(ward));

        assertThatThrownBy(() -> validator.validate(null, 7L))
                .isInstanceOf(WardAccessDeniedException.class);
    }
}
