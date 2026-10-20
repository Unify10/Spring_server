package com.unify.onyak.domain.medications.error.exception;

import com.unify.onyak.domain.medications.error.MedicationErrorCode;
import com.unify.onyak.global.error.CustomException;

public class WardNotFoundException extends CustomException {
    public WardNotFoundException() {
        super(MedicationErrorCode.WARD_NOT_FOUND);
    }
}
