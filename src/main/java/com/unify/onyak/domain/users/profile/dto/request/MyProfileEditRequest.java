package com.unify.onyak.domain.users.profile.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MyProfileEditRequest(
        @NotBlank
        @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
        String phone,

        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상, 64자 이하 입니다.")
        String password,

        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상, 64자 이하 입니다.")
        String currentPassword
) {
}
