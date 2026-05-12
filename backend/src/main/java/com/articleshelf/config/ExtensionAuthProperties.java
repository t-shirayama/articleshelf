package com.articleshelf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "articleshelf.extension-auth")
public record ExtensionAuthProperties(
        long codeTtlSeconds,
        long accessTokenTtlSeconds,
        List<Client> clients
) {
    public record Client(
            String clientId,
            String extensionId,
            String redirectUri
    ) {
    }
}
