package com.articleshelf.application.extension;

import java.util.Optional;

public interface ExtensionClientRegistry {
    Optional<ExtensionClient> findByClientId(String clientId);
}
