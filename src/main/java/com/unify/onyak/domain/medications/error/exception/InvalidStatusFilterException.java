package com.unify.onyak.domain.medications.error.exception;

import com.unify.onyak.domain.medications.error.MedicationErrorCode;
import com.unify.onyak.global.error.CustomException;

public class InvalidStatusFilterException extends CustomException {
    public InvalidStatusFilterException() {
        super(MedicationErrorCode.MEDICATION_INVALID_STATUS_FILTER);
    }
}
