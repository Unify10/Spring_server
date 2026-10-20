package com.unify.onyak.domain.medications.error.exception;

import com.unify.onyak.domain.medications.error.MedicationErrorCode;
import com.unify.onyak.global.error.CustomException;

public class EndDateOnActiveMedicationException extends CustomException {
    public EndDateOnActiveMedicationException() {
        super(MedicationErrorCode.MEDICATION_END_DATE_ON_ACTIVE);
    }
}
