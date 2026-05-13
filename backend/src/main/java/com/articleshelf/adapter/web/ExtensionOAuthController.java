package com.articleshelf.adapter.web;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.application.extension.ExtensionAuthService;
import com.articleshelf.application.extension.ExtensionTokenResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/extension/oauth")
public class ExtensionOAuthController {
    private final ExtensionAuthService extensionAuthService;

    public ExtensionOAuthController(ExtensionAuthService extensionAuthService) {
        this.extensionAuthService = extensionAuthService;
    }

    @PostMapping("/authorize")
    public AuthorizeResponse authorize(
            @AuthenticationPrincipal CurrentUser user,
            @Valid @RequestBody AuthorizeRequest request
    ) {
        String code = extensionAuthService.authorize(
                user,
                request.clientId(),
                request.extensionId(),
                request.redirectUri(),
                request.codeChallenge(),
                request.codeChallengeMethod()
        );
        return new AuthorizeResponse(request.redirectUri(), code, request.state());
    }

    @PostMapping("/token")
    public TokenResponse token(@Valid @RequestBody TokenRequest request) {
        ExtensionTokenResponse response = extensionAuthService.exchangeToken(
                request.grantType(),
                request.code(),
                request.redirectUri(),
                request.clientId(),
                request.codeVerifier()
        );
        return new TokenResponse(response.accessToken(), response.tokenType(), response.expiresIn(), response.scope());
    }

    public record AuthorizeRequest(
            @NotBlank String clientId,
            @NotBlank String extensionId,
            @NotBlank String redirectUri,
            @NotBlank String state,
            @NotBlank String codeChallenge,
            @NotBlank String codeChallengeMethod
    ) {
    }

    public record AuthorizeResponse(String redirectUri, String code, String state) {
    }

    public record TokenRequest(
            @JsonProperty("grant_type") @NotBlank String grantType,
            @NotBlank String code,
            @JsonProperty("redirect_uri") @NotBlank String redirectUri,
            @JsonProperty("client_id") @NotBlank String clientId,
            @JsonProperty("code_verifier") @NotBlank String codeVerifier
    ) {
    }

    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn,
            String scope
    ) {
    }
}
