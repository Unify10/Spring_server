package com.unify.onyak.domain.medications.error.exception;

import com.unify.onyak.domain.medications.error.MedicationErrorCode;
import com.unify.onyak.global.error.CustomException;

public class InactiveMedicationException extends CustomException {
    public InactiveMedicationException() {
        super(MedicationErrorCode.MEDICATION_INACTIVE);
    }
}
