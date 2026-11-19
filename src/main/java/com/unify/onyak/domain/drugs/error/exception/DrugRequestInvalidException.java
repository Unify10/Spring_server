package com.unify.onyak.domain.drugs.error.exception;

import com.unify.onyak.domain.drugs.error.DrugErrorCode;
import com.unify.onyak.global.error.CustomException;

public class DrugRequestInvalidException extends CustomException {
    public DrugRequestInvalidException() {
        super(DrugErrorCode.DRUG_REQUEST_INVALID);
    }
}
