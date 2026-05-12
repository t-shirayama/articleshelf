package com.articleshelf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "articleshelf.ogp")
public record OgpProperties(
        String proxyUrl,
        boolean requireProxyInProd
) {
}
