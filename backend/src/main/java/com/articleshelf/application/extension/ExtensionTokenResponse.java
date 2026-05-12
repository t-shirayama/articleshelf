package com.articleshelf.application.extension;

public record ExtensionTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String scope
) {
}
