package com.unify.onyak.domain.medications.error.exception;

import com.unify.onyak.domain.medications.error.MedicationCheckErrorCode;
import com.unify.onyak.global.error.CustomException;

public class CheckTimeNotScheduledException extends CustomException {
    public CheckTimeNotScheduledException() {
        super(MedicationCheckErrorCode.CHECK_TIME_NOT_SCHEDULED);
    }
}