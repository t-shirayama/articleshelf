package com.articleshelf.application.extension;

import java.time.Instant;
import java.util.Optional;

public interface ExtensionAuthCodeRepository {
    ExtensionAuthCode save(ExtensionAuthCode code);

    Optional<ExtensionAuthCode> findByCodeHash(String codeHash);

    void markConsumed(String codeHash, Instant consumedAt);
}
