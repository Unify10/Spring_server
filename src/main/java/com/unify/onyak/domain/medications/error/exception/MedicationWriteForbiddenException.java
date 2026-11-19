package com.unify.onyak.domain.medications.error.exception;

import com.unify.onyak.domain.medications.error.MedicationErrorCode;
import com.unify.onyak.global.error.CustomException;

public class MedicationWriteForbiddenException extends CustomException {
    public MedicationWriteForbiddenException() {
        super(MedicationErrorCode.MEDICATION_WRITE_FORBIDDEN);
    }
}
