package com.unify.onyak.domain.medications.policy;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanDateTest {

    @Test
    void java_sql_Date도_예외없이_만나이를_계산한다() {
        // JPA 가 DATE 컬럼을 java.sql.Date 로 채운다. toInstant() 를 쓰면 예외가 난다.
        Date birth = java.sql.Date.valueOf("1948-03-01");

        assertThat(KoreanDate.ageOn(birth, LocalDate.of(2026, 2, 28))).isEqualTo(77);
        assertThat(KoreanDate.ageOn(birth, LocalDate.of(2026, 3, 1))).isEqualTo(78);
    }

    @Test
    void java_util_Date도_처리한다() {
        Date birth = Date.from(LocalDate.of(1990, 5, 20).atStartOfDay(KoreanDate.ZONE).toInstant());

        assertThat(KoreanDate.ageOn(birth, LocalDate.of(2026, 10, 13))).isEqualTo(36);
    }

    @Test
    void 생년월일이_없으면_null() {
        assertThat(KoreanDate.ageOn(null, LocalDate.of(2026, 10, 13))).isNull();
    }
}
