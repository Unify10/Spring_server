package com.unify.onyak.domain.medications.error.exception;

import com.unify.onyak.domain.medications.error.MedicationCheckErrorCode;
import com.unify.onyak.global.error.CustomException;

public class CheckAlreadyDoneException extends CustomException {
    public CheckAlreadyDoneException() {
        super(MedicationCheckErrorCode.CHECK_ALREADY_DONE);
    }
}