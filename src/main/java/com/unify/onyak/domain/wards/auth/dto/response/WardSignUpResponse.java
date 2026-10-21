package com.unify.onyak.domain.wards.auth.dto.response;

import com.unify.onyak.domain.wards.link_codes.response.LinkCodeResponse;

public record WardSignUpResponse(
        Long wardId,

        String name,

        LinkCodeResponse linkCodeResponse
){
}
