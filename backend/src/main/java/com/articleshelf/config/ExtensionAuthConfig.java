package com.articleshelf.config;

import com.articleshelf.application.extension.ExtensionAuthSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExtensionAuthConfig {
    @Bean
    public ExtensionAuthSettings extensionAuthSettings(ExtensionAuthProperties properties) {
        return new ExtensionAuthSettings(properties.codeTtlSeconds(), properties.accessTokenTtlSeconds());
    }
}
