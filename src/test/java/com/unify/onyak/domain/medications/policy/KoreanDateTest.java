package com.unify.onyak.domain.medications.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanDateTest {

    @Test
    void 생일_전날까지는_한_살_적고_생일에_나이가_오른다() {
        LocalDate birth = LocalDate.of(1948, 3, 1);

        assertThat(KoreanDate.ageOn(birth, LocalDate.of(2026, 2, 28))).isEqualTo(77);
        assertThat(KoreanDate.ageOn(birth, LocalDate.of(2026, 3, 1))).isEqualTo(78);
    }

    @Test
    void 생년월일이_없으면_null() {
        assertThat(KoreanDate.ageOn(null, LocalDate.of(2026, 10, 13))).isNull();
    }

    @Test
    void 생년월일이_미래면_0살() {
        assertThat(KoreanDate.ageOn(LocalDate.of(2030, 1, 1), LocalDate.of(2026, 10, 13))).isEqualTo(0);
    }
}
