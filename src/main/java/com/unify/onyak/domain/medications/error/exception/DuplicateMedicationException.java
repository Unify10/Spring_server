package com.unify.onyak.domain.medications.error.exception;

import com.unify.onyak.domain.medications.error.MedicationErrorCode;
import com.unify.onyak.global.error.CustomException;

public class DuplicateMedicationException extends CustomException {
    public DuplicateMedicationException() {
        super(MedicationErrorCode.MEDICATION_DUPLICATE);
    }
}
