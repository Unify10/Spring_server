package com.unify.onyak.domain.medications.error.exception;

import com.unify.onyak.domain.medications.error.MedicationErrorCode;
import com.unify.onyak.global.error.CustomException;

public class InvalidEndDateException extends CustomException {
    public InvalidEndDateException() {
        super(MedicationErrorCode.MEDICATION_INVALID_END_DATE);
    }
}
