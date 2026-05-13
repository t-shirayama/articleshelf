package com.articleshelf.application.extension;

import java.util.Optional;

public interface ExtensionAccessTokenRepository {
    ExtensionAccessToken save(ExtensionAccessToken token);

    Optional<ExtensionAccessToken> findByTokenHash(String tokenHash);
}
