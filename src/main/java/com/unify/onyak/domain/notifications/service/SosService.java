package com.unify.onyak.domain.notifications.service;


import com.unify.onyak.domain.notifications.dto.request.SosRequest;

public interface SosService {
    void sendSos(Long wardId, SosRequest request);
}