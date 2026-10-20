package com.unify.onyak.domain.medications.policy;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

/** 날짜 계산은 전부 한국 시간 기준으로 한다. */
public final class KoreanDate {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private KoreanDate() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /**
     * Ward.birthDate(java.util.Date) → LocalDate.
     * JPA 가 DATE 컬럼을 java.sql.Date 로 채우는데, java.sql.Date#toInstant() 는
     * UnsupportedOperationException 을 던지므로 따로 처리한다.
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return date.toInstant().atZone(ZONE).toLocalDate();
    }

    /** 만 나이. 생년월일이 없으면 null. */
    public static Integer ageOn(Date birthDate, LocalDate on) {
        LocalDate birth = toLocalDate(birthDate);
        if (birth == null) {
            return null;
        }
        return Math.max(Period.between(birth, on).getYears(), 0);
    }
}
