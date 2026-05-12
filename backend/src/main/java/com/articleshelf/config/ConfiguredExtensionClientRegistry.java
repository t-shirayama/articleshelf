package com.articleshelf.config;

import com.articleshelf.application.extension.ExtensionClient;
import com.articleshelf.application.extension.ExtensionClientRegistry;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConfiguredExtensionClientRegistry implements ExtensionClientRegistry {
    private final ExtensionAuthProperties properties;

    public ConfiguredExtensionClientRegistry(ExtensionAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<ExtensionClient> findByClientId(String clientId) {
        return properties.clients().stream()
                .filter(client -> client.clientId().equals(clientId))
                .findFirst()
                .map(client -> new ExtensionClient(client.clientId(), client.extensionId(), client.redirectUri()));
    }
}
