package com.unify.onyak.domain.medications.policy;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

/** 날짜 계산은 전부 한국 시간 기준으로 한다. */
public final class KoreanDate {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private KoreanDate() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** 만 나이. 생년월일이 없으면 null. */
    public static Integer ageOn(LocalDate birth, LocalDate on) {
        if (birth == null) {
            return null;
        }
        return Math.max(Period.between(birth, on).getYears(), 0);
    }
}
