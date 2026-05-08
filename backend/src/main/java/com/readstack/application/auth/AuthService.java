package com.readstack.application.auth;

import com.readstack.domain.user.PasswordPolicy;
import com.readstack.domain.user.UserStatus;
import com.readstack.domain.user.UsernamePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final AuthUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenSecretService refreshTokenSecretService;
    private final AuthSettings settings;
    private final UsernamePolicy usernamePolicy = new UsernamePolicy();
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            AuthUserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer,
            RefreshTokenSecretService refreshTokenSecretService,
            AuthSettings settings
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenSecretService = refreshTokenSecretService;
        this.settings = settings;
    }

    @Transactional
    public AuthResult register(String username, String password, String displayName, String userAgent, String ipAddress) {
        String normalizedUsername = normalizeUsername(username);
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new DuplicateUsernameException(normalizedUsername);
        }
        usernamePolicy.validate(normalizedUsername);
        passwordPolicy.validate(normalizedUsername, password);

        AuthUser user = new AuthUser(
                null,
                normalizedUsername,
                passwordHasher.hash(password),
                normalizeDisplayName(displayName, normalizedUsername),
                "USER",
                UserStatus.ACTIVE,
                Instant.now(),
                Instant.EPOCH
        );
        return issueAuthResult(userRepository.save(user), UUID.randomUUID(), userAgent, ipAddress);
    }

    @Transactional
    public AuthResult login(String username, String password, String userAgent, String ipAddress) {
        AuthUser user = userRepository.findByUsername(normalizeUsername(username))
                .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
                .filter(candidate -> passwordHasher.matches(password == null ? "" : password, candidate.passwordHash()))
                .orElseThrow(() -> new AuthException(
                        AuthException.Reason.INVALID_CREDENTIALS,
                        "username or password is incorrect"
                ));
        user = userRepository.save(new AuthUser(
                user.id(),
                user.username(),
                user.passwordHash(),
                user.displayName(),
                user.role(),
                user.status(),
                Instant.now(),
                user.tokenValidAfter()
        ));
        return issueAuthResult(user, UUID.randomUUID(), userAgent, ipAddress);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw AuthException.invalidRefreshToken("refresh token is missing");
        }
        Instant now = Instant.now();
        RefreshTokenRecord current = refreshTokenRepository.findByTokenHash(refreshTokenSecretService.hash(rawRefreshToken))
                .orElseThrow(() -> AuthException.invalidRefreshToken("refresh token is invalid"));
        AuthUser user = current.user();
        if (user.status() != UserStatus.ACTIVE || current.expiresAt().isBefore(now)) {
            throw AuthException.invalidRefreshToken("refresh token is invalid");
        }
        if (current.revokedAt() != null) {
            refreshTokenRepository.revokeFamily(user.id(), current.familyId(), now);
            throw AuthException.invalidRefreshToken("refresh token is invalid");
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

    @Transactional
    public void logoutAll(CurrentUser currentUser) {
        AuthUser user = requireActiveUser(currentUser);
        refreshTokenRepository.revokeAllByUserId(user.id(), Instant.now());
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(CurrentUser currentUser) {
        return UserResponse.from(requireActiveUser(currentUser));
    }

    @Transactional
    public void changePassword(CurrentUser currentUser, String currentPassword, String newPassword) {
        AuthUser user = requireActiveUser(currentUser);
        if (!passwordHasher.matches(currentPassword == null ? "" : currentPassword, user.passwordHash())) {
            throw new AuthException(AuthException.Reason.INVALID_CREDENTIALS, "current password is incorrect");
        }
        passwordPolicy.validate(user.username(), newPassword);
        saveWithPasswordAndInvalidatedTokens(user, newPassword, UserStatus.ACTIVE);
    }

    @Transactional
    public void deleteAccount(CurrentUser currentUser, String currentPassword) {
        AuthUser user = requireActiveUser(currentUser);
        if (!passwordHasher.matches(currentPassword == null ? "" : currentPassword, user.passwordHash())) {
            throw new AuthException(AuthException.Reason.INVALID_CREDENTIALS, "current password is incorrect");
        }
        saveWithPasswordAndInvalidatedTokens(user, null, UserStatus.DELETED);
    }

    @Transactional
    public void resetPasswordByAdmin(String username, String newPassword) {
        AuthUser user = userRepository.findByUsername(normalizeUsername(username))
                .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AccountNotFoundException(username));
        passwordPolicy.validate(user.username(), newPassword);
        saveWithPasswordAndInvalidatedTokens(user, newPassword, UserStatus.ACTIVE);
    }

    @Transactional
    public Optional<AuthUser> ensureInitialUser() {
        if (!settings.initialUserEnabled()) {
            return Optional.empty();
        }
        String username = normalizeUsername(settings.initialUsername());
        usernamePolicy.validate(username);
        return Optional.of(userRepository.findByUsername(username).orElseGet(() -> {
            passwordPolicy.validate(username, settings.initialUserPassword());
            AuthUser user = new AuthUser(
                    null,
                    username,
                    passwordHasher.hash(settings.initialUserPassword()),
                    "ReadStack Owner",
                    "ADMIN",
                    UserStatus.ACTIVE,
                    null,
                    Instant.EPOCH
            );
            return userRepository.save(user);
        }));
    }

    private AuthUser requireActiveUser(CurrentUser currentUser) {
        if (currentUser == null) {
            throw new AuthException(AuthException.Reason.USER_INACTIVE, "user is not active");
        }
        return userRepository.findById(currentUser.id())
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AuthException(AuthException.Reason.USER_INACTIVE, "user is not active"));
    }

    private void saveWithPasswordAndInvalidatedTokens(AuthUser user, String newPassword, UserStatus status) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        userRepository.save(new AuthUser(
                user.id(),
                user.username(),
                newPassword == null ? user.passwordHash() : passwordHasher.hash(newPassword),
                user.displayName(),
                user.role(),
                status,
                user.lastLoginAt(),
                now
        ));
        refreshTokenRepository.revokeAllByUserId(user.id(), now);
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
                Instant.now().plus(settings.refreshTokenTtlDays(), ChronoUnit.DAYS),
                userAgent,
                ipAddress
        );
        return new CreatedRefreshToken(rawToken, token);
    }

    private AuthResponse toAuthResponse(AuthUser user) {
        CurrentUser currentUser = new CurrentUser(user.id(), user.username(), user.displayName(), List.of(user.role()));
        return new AuthResponse(UserResponse.from(user), accessTokenIssuer.issue(currentUser));
    }

    private String issueCsrfToken() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalizeUsername(String username) {
        return usernamePolicy.normalize(username);
    }

    private String normalizeDisplayName(String displayName, String username) {
        return displayName != null && !displayName.isBlank() ? displayName.trim() : username;
    }

    private record CreatedRefreshToken(String rawToken, RefreshTokenRecord record) {
    }
}
