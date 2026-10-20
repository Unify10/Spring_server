package com.unify.onyak.domain.medications.error.exception;

import com.unify.onyak.domain.medications.error.MedicationErrorCode;
import com.unify.onyak.global.error.CustomException;

public class MedicationNotFoundException extends CustomException {
    public MedicationNotFoundException() {
        super(MedicationErrorCode.MEDICATION_NOT_FOUND);
    }
}
