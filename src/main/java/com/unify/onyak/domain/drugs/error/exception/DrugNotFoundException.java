package com.unify.onyak.domain.drugs.error.exception;

import com.unify.onyak.domain.drugs.error.DrugErrorCode;
import com.unify.onyak.global.error.CustomException;

public class DrugNotFoundException extends CustomException {
    public DrugNotFoundException() {
        super(DrugErrorCode.DRUG_NOT_FOUND);
    }
}
