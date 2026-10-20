package com.unify.onyak.domain.medications.error.exception;

import com.unify.onyak.domain.medications.error.MedicationErrorCode;
import com.unify.onyak.global.error.CustomException;

public class InvalidScheduleTimeException extends CustomException {
    public InvalidScheduleTimeException() {
        super(MedicationErrorCode.SCHEDULE_TIME_INVALID);
    }
}
