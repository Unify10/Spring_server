package com.unify.onyak.domain.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.info")
public record AppInfoProperties(
        String version,
        String noticeUrl,
        String termsUrl,
        String privacyUrl
)
{
}
