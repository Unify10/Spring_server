package com.unify.onyak.domain.users.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Gender {
    Man("남자"),
    Woman("여자");

    private final String displayName;
}
