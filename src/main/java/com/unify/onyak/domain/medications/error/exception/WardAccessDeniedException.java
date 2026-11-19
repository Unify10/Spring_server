package com.unify.onyak.domain.medications.error.exception;

import com.unify.onyak.domain.medications.error.MedicationErrorCode;
import com.unify.onyak.global.error.CustomException;

public class WardAccessDeniedException extends CustomException {
    public WardAccessDeniedException() {
        super(MedicationErrorCode.WARD_ACCESS_DENIED);
    }
}
