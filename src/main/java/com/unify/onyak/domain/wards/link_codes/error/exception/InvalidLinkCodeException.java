package com.unify.onyak.domain.wards.link_codes.error.exception;

import com.unify.onyak.domain.wards.link_codes.error.LinkCodeErrorCode;
import com.unify.onyak.global.error.CustomException;

public class InvalidLinkCodeException extends CustomException {
    public InvalidLinkCodeException() {
        super(LinkCodeErrorCode.INVALID_LINK_CODE_REQUEST);
    }
}
