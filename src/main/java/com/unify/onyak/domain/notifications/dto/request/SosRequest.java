package com.unify.onyak.domain.notifications.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SosRequest {
    private Double locationLat;
    private Double locationLng;
}