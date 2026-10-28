package com.unify.onyak.domain.wards.link_codes.error.exception;

import com.unify.onyak.domain.wards.link_codes.error.LinkCodeErrorCode;
import com.unify.onyak.global.error.CustomException;

public class LinkCodeGenerationFailedException extends CustomException {
    public LinkCodeGenerationFailedException(){
        super(LinkCodeErrorCode.LINK_CODE_GENERATION_FAILED);
    };
}
