package com.articleshelf.application.extension;

public record ExtensionClient(
        String clientId,
        String extensionId,
        String redirectUri
) {
}
