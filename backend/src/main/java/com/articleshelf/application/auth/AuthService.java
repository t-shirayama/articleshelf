package com.articleshelf.application.auth;

import com.articleshelf.domain.user.PasswordPolicy;
import com.articleshelf.domain.user.UserStatus;
import com.articleshelf.domain.user.UsernamePolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private final AuthUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenSecretService refreshTokenSecretService;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final RefreshTokenRotationService refreshTokenRotationService;
    private final InitialUserProvisioner initialUserProvisioner;
    private final UsernamePolicy usernamePolicy = new UsernamePolicy();
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    public AuthService(
            AuthUserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasher passwordHasher,
            AccessTokenIssuer accessTokenIssuer,
            RefreshTokenSecretService refreshTokenSecretService,
            AuthSettings settings,
            Clock clock,
            IdGenerator idGenerator,
            SecureRandom secureRandom
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenSecretService = refreshTokenSecretService;
        this.clock = clock;
        this.idGenerator = idGenerator;
        this.refreshTokenRotationService = new RefreshTokenRotationService(
                refreshTokenRepository,
                accessTokenIssuer,
                refreshTokenSecretService,
                settings,
                clock,
                secureRandom
        );
        this.initialUserProvisioner = new InitialUserProvisioner(userRepository, passwordHasher, settings);
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
                now(),
                Instant.EPOCH
        );
        return issueAuthResult(userRepository.save(user), idGenerator.nextUuid(), userAgent, ipAddress);
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
                now(),
                user.tokenValidAfter()
        ));
        return issueAuthResult(user, idGenerator.nextUuid(), userAgent, ipAddress);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        return refreshTokenRotationService.rotate(rawRefreshToken, userAgent, ipAddress);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(refreshTokenSecretService.hash(rawRefreshToken))
                .ifPresent(token -> {
                    if (token.revokedAt() == null) {
                        refreshTokenRepository.revoke(token.id(), now());
                    }
                });
    }

    @Transactional
    public void logoutAll(CurrentUser currentUser) {
        AuthUser user = requireActiveUser(currentUser);
        invalidateTokens(user);
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
        return initialUserProvisioner.ensureInitialUser();
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
        Instant now = now().truncatedTo(ChronoUnit.SECONDS);
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

    private void invalidateTokens(AuthUser user) {
        Instant now = now().truncatedTo(ChronoUnit.SECONDS);
        userRepository.save(new AuthUser(
                user.id(),
                user.username(),
                user.passwordHash(),
                user.displayName(),
                user.role(),
                user.status(),
                user.lastLoginAt(),
                now
        ));
        refreshTokenRepository.revokeAllByUserId(user.id(), now);
    }

    private AuthResult issueAuthResult(AuthUser user, UUID familyId, String userAgent, String ipAddress) {
        return refreshTokenRotationService.issue(user, familyId, userAgent, ipAddress);
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private String normalizeUsername(String username) {
        return usernamePolicy.normalize(username);
    }

    private String normalizeDisplayName(String displayName, String username) {
        return displayName != null && !displayName.isBlank() ? displayName.trim() : username;
    }
}
