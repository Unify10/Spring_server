package com.unify.onyak.domain.users.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Gender {
    M("남자"),
    F("여자");

    private final String displayName;
}
