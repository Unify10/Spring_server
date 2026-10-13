package com.unify.onyak.domain.drugs.error.exception;

import com.unify.onyak.domain.drugs.error.DrugErrorCode;
import com.unify.onyak.global.error.CustomException;

public class DrugApiUnavailableException extends CustomException {
    public DrugApiUnavailableException() {
        super(DrugErrorCode.DRUG_API_UNAVAILABLE);
    }

    public DrugApiUnavailableException(Throwable cause) {
        super(DrugErrorCode.DRUG_API_UNAVAILABLE, cause);
    }
}
