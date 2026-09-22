package com.unify.onyak.domain.wards.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SignUpRequest {
    @NotBlank
    String name;


}
