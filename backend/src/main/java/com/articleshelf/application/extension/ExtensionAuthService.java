package com.articleshelf.application.extension;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.application.auth.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class ExtensionAuthService {
    public static final String SCOPE_LOOKUP = "article:lookup";
    public static final String SCOPE_CREATE = "article:create";
    public static final String SCOPE_UPDATE_STATUS = "article:update_status";
    public static final String DEFAULT_SCOPE = String.join(" ", List.of(SCOPE_LOOKUP, SCOPE_CREATE, SCOPE_UPDATE_STATUS));

    private final ExtensionClientRegistry clientRegistry;
    private final ExtensionAuthCodeRepository codeRepository;
    private final ExtensionAccessTokenRepository tokenRepository;
    private final ExtensionAuthSettings settings;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final SecureRandom secureRandom;

    public ExtensionAuthService(
            ExtensionClientRegistry clientRegistry,
            ExtensionAuthCodeRepository codeRepository,
            ExtensionAccessTokenRepository tokenRepository,
            ExtensionAuthSettings settings,
            IdGenerator idGenerator,
            Clock clock,
            SecureRandom secureRandom
    ) {
        this.clientRegistry = clientRegistry;
        this.codeRepository = codeRepository;
        this.tokenRepository = tokenRepository;
        this.settings = settings;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    @Transactional
    public String authorize(
            CurrentUser user,
            String clientId,
            String extensionId,
            String redirectUri,
            String codeChallenge,
            String codeChallengeMethod
    ) {
        ExtensionClient client = validateClient(clientId, extensionId, redirectUri);
        if (!"S256".equals(codeChallengeMethod) || codeChallenge == null || codeChallenge.isBlank()) {
            throw new ExtensionAuthException("PKCE S256 is required");
        }

        String code = randomToken();
        Instant now = clock.instant();
        codeRepository.save(new ExtensionAuthCode(
                idGenerator.nextUuid(),
                hash(code),
                user.id(),
                client.clientId(),
                client.extensionId(),
                client.redirectUri(),
                codeChallenge,
                codeChallengeMethod,
                now.plusSeconds(settings.codeTtlSeconds()),
                null,
                now
        ));
        return code;
    }

    @Transactional
    public ExtensionTokenResponse exchangeToken(
            String grantType,
            String code,
            String redirectUri,
            String clientId,
            String codeVerifier
    ) {
        if (!"authorization_code".equals(grantType)) {
            throw new ExtensionAuthException("unsupported grant type");
        }
        if (code == null || codeVerifier == null) {
            throw new ExtensionAuthException("invalid token request");
        }

        ExtensionAuthCode authCode = codeRepository.findByCodeHash(hash(code))
                .orElseThrow(() -> new ExtensionAuthException("invalid authorization code"));
        ExtensionClient client = clientRegistry.findByClientId(clientId)
                .orElseThrow(() -> new ExtensionAuthException("unknown client"));
        Instant now = clock.instant();
        if (!authCode.isUsableAt(now)
                || !authCode.clientId().equals(client.clientId())
                || !authCode.extensionId().equals(client.extensionId())
                || !authCode.redirectUri().equals(redirectUri)
                || !verifyPkce(codeVerifier, authCode.codeChallenge())) {
            throw new ExtensionAuthException("invalid authorization code");
        }

        codeRepository.markConsumed(authCode.codeHash(), now);

        String accessToken = randomToken();
        tokenRepository.save(new ExtensionAccessToken(
                idGenerator.nextUuid(),
                hash(accessToken),
                authCode.userId(),
                authCode.clientId(),
                authCode.extensionId(),
                DEFAULT_SCOPE,
                now.plusSeconds(settings.accessTokenTtlSeconds()),
                null,
                now
        ));
        return new ExtensionTokenResponse(accessToken, "Bearer", settings.accessTokenTtlSeconds(), DEFAULT_SCOPE);
    }

    public String tokenHash(String token) {
        return hash(token);
    }

    private ExtensionClient validateClient(String clientId, String extensionId, String redirectUri) {
        ExtensionClient client = clientRegistry.findByClientId(clientId)
                .orElseThrow(() -> new ExtensionAuthException("unknown client"));
        if (!client.extensionId().equals(extensionId) || !client.redirectUri().equals(redirectUri)) {
            throw new ExtensionAuthException("invalid client");
        }
        return client;
    }

    private boolean verifyPkce(String verifier, String challenge) {
        return base64Url(sha256(verifier)).equals(challenge);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return base64Url(bytes);
    }

    private String hash(String value) {
        return base64Url(sha256(value));
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
