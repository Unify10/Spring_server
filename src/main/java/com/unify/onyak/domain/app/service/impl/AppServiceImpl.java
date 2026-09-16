package com.unify.onyak.domain.app.service.impl;

import com.unify.onyak.domain.app.config.AppInfoProperties;
import com.unify.onyak.domain.app.dto.response.AppInfoResponse;
import com.unify.onyak.domain.app.service.AppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppServiceImpl implements AppService {
    private final AppInfoProperties appInfoProperties;

    @Override
    public AppInfoResponse getAppInfo() {
        return AppInfoResponse.builder()
                .version(appInfoProperties.version())
                .noticeUrl(appInfoProperties.noticeUrl())
                .termsUrl(appInfoProperties.termsUrl())
                .privacyUrl(appInfoProperties.privacyUrl())
                .build();
    }
}
