package com.readstack.application.auth;

import com.readstack.config.AuthProperties;
import com.readstack.domain.user.PasswordPolicy;
import com.readstack.domain.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final AuthUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenSecretService refreshTokenSecretService;
    private final AuthProperties properties;
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            AuthUserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer,
            RefreshTokenSecretService refreshTokenSecretService,
            AuthProperties properties
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenSecretService = refreshTokenSecretService;
        this.properties = properties;
    }

    @Transactional
    public AuthResult register(String email, String password, String displayName, String userAgent, String ipAddress) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }
        passwordPolicy.validate(normalizedEmail, password);

        AuthUser user = new AuthUser(
                null,
                normalizedEmail,
                passwordHasher.hash(password),
                normalizeDisplayName(displayName, normalizedEmail),
                "USER",
                UserStatus.ACTIVE,
                Instant.now()
        );
        return issueAuthResult(userRepository.save(user), UUID.randomUUID(), userAgent, ipAddress);
    }

    @Transactional
    public AuthResult login(String email, String password, String userAgent, String ipAddress) {
        AuthUser user = userRepository.findByEmail(normalizeEmail(email))
                .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
                .filter(candidate -> passwordHasher.matches(password == null ? "" : password, candidate.passwordHash()))
                .orElseThrow(() -> new AuthException("メールアドレスまたはパスワードが正しくありません"));
        user = userRepository.save(new AuthUser(
                user.id(),
                user.email(),
                user.passwordHash(),
                user.displayName(),
                user.role(),
                user.status(),
                Instant.now()
        ));
        return issueAuthResult(user, UUID.randomUUID(), userAgent, ipAddress);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthException("refresh token is missing");
        }
        Instant now = Instant.now();
        RefreshTokenRecord current = refreshTokenRepository.findByTokenHash(refreshTokenSecretService.hash(rawRefreshToken))
                .orElseThrow(() -> new AuthException("refresh token is invalid"));
        AuthUser user = current.user();
        if (user.status() != UserStatus.ACTIVE || current.expiresAt().isBefore(now)) {
            throw new AuthException("refresh token is invalid");
        }
        if (current.revokedAt() != null) {
            refreshTokenRepository.revokeFamily(user.id(), current.familyId(), now);
            throw new AuthException("refresh token is invalid");
        }

        CreatedRefreshToken replacement = createRefreshToken(user, current.familyId(), userAgent, ipAddress);
        refreshTokenRepository.replace(current.id(), replacement.record().id(), now);
        return new AuthResult(toAuthResponse(user), new RefreshSession(replacement.rawToken(), issueCsrfToken()));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(refreshTokenSecretService.hash(rawRefreshToken))
                .ifPresent(token -> {
                    if (token.revokedAt() == null) {
                        refreshTokenRepository.revoke(token.id(), Instant.now());
                    }
                });
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(CurrentUser currentUser) {
        return userRepository.findById(currentUser.id())
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .map(UserResponse::from)
                .orElseThrow(() -> new AuthException("user is not active"));
    }

    @Transactional
    public AuthUser ensureInitialUser() {
        String email = normalizeEmail(properties.initialUserEmail());
        return userRepository.findByEmail(email).orElseGet(() -> {
            passwordPolicy.validate(email, properties.initialUserPassword());
            AuthUser user = new AuthUser(
                    null,
                    email,
                    passwordHasher.hash(properties.initialUserPassword()),
                    "ReadStack Owner",
                    "USER",
                    UserStatus.ACTIVE,
                    null
            );
            return userRepository.save(user);
        });
    }

    private AuthResult issueAuthResult(AuthUser user, UUID familyId, String userAgent, String ipAddress) {
        CreatedRefreshToken refreshToken = createRefreshToken(user, familyId, userAgent, ipAddress);
        return new AuthResult(toAuthResponse(user), new RefreshSession(refreshToken.rawToken(), issueCsrfToken()));
    }

    private CreatedRefreshToken createRefreshToken(AuthUser user, UUID familyId, String userAgent, String ipAddress) {
        String rawToken = refreshTokenSecretService.generateRawToken();
        RefreshTokenRecord token = refreshTokenRepository.create(
                user,
                refreshTokenSecretService.hash(rawToken),
                familyId,
                Instant.now().plus(properties.refreshTokenTtlDays(), ChronoUnit.DAYS),
                userAgent,
                ipAddress
        );
        return new CreatedRefreshToken(rawToken, token);
    }

    private AuthResponse toAuthResponse(AuthUser user) {
        CurrentUser currentUser = new CurrentUser(user.id(), user.email(), user.displayName(), List.of(user.role()));
        return new AuthResponse(UserResponse.from(user), accessTokenIssuer.issue(currentUser));
    }

    private String issueCsrfToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDisplayName(String displayName, String email) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private record CreatedRefreshToken(String rawToken, RefreshTokenRecord record) {
    }
}
