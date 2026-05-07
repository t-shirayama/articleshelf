package com.readstack.application.auth;

import com.readstack.config.AuthProperties;
import com.readstack.domain.user.PasswordPolicy;
import com.readstack.domain.user.UserStatus;
import com.readstack.infrastructure.persistence.RefreshTokenEntity;
import com.readstack.infrastructure.persistence.SpringDataRefreshTokenJpaRepository;
import com.readstack.infrastructure.persistence.SpringDataUserJpaRepository;
import com.readstack.infrastructure.persistence.UserEntity;
import com.readstack.infrastructure.security.JwtTokenService;
import com.readstack.infrastructure.security.RefreshTokenHashService;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final SpringDataUserJpaRepository userRepository;
    private final SpringDataRefreshTokenJpaRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenHashService refreshTokenHashService;
    private final AuthProperties properties;
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            SpringDataUserJpaRepository userRepository,
            SpringDataRefreshTokenJpaRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenHashService refreshTokenHashService,
            AuthProperties properties
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenHashService = refreshTokenHashService;
        this.properties = properties;
    }

    @Transactional
    public AuthResult register(String email, String password, String displayName, String userAgent, String ipAddress) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }
        passwordPolicy.validate(normalizedEmail, password);

        UserEntity user = new UserEntity();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(normalizeDisplayName(displayName, normalizedEmail));
        user.setRole("USER");
        user.setStatus(UserStatus.ACTIVE);
        user.setLastLoginAt(Instant.now());
        return issueAuthResult(userRepository.save(user), UUID.randomUUID(), userAgent, ipAddress);
    }

    @Transactional
    public AuthResult login(String email, String password, String userAgent, String ipAddress) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(email))
                .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                .filter(candidate -> passwordEncoder.matches(password == null ? "" : password, candidate.getPasswordHash()))
                .orElseThrow(() -> new AuthException("メールアドレスまたはパスワードが正しくありません"));
        user.setLastLoginAt(Instant.now());
        return issueAuthResult(user, UUID.randomUUID(), userAgent, ipAddress);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken, String userAgent, String ipAddress) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AuthException("refresh token is missing");
        }
        Instant now = Instant.now();
        RefreshTokenEntity current = refreshTokenRepository.findByTokenHash(refreshTokenHashService.hash(rawRefreshToken))
                .orElseThrow(() -> new AuthException("refresh token is invalid"));
        UserEntity user = current.getUser();
        if (user.getStatus() != UserStatus.ACTIVE || current.getExpiresAt().isBefore(now)) {
            throw new AuthException("refresh token is invalid");
        }
        if (current.getRevokedAt() != null) {
            refreshTokenRepository.revokeFamily(user.getId(), current.getFamilyId(), now);
            throw new AuthException("refresh token is invalid");
        }

        CreatedRefreshToken replacement = createRefreshToken(user, current.getFamilyId(), userAgent, ipAddress);
        current.setRevokedAt(now);
        current.setReplacedByTokenId(replacement.entity().getId());
        refreshTokenRepository.save(current);
        return new AuthResult(toAuthResponse(user), new RefreshSession(replacement.rawToken(), issueCsrfToken()));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(refreshTokenHashService.hash(rawRefreshToken))
                .ifPresent(token -> {
                    if (token.getRevokedAt() == null) {
                        token.setRevokedAt(Instant.now());
                        refreshTokenRepository.save(token);
                    }
                });
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(CurrentUser currentUser) {
        return userRepository.findById(currentUser.id())
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(UserResponse::from)
                .orElseThrow(() -> new AuthException("user is not active"));
    }

    @Transactional
    public UserEntity ensureInitialUser() {
        String email = normalizeEmail(properties.initialUserEmail());
        return userRepository.findByEmail(email).orElseGet(() -> {
            passwordPolicy.validate(email, properties.initialUserPassword());
            UserEntity user = new UserEntity();
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(properties.initialUserPassword()));
            user.setDisplayName("ReadStack Owner");
            user.setRole("USER");
            user.setStatus(UserStatus.ACTIVE);
            return userRepository.save(user);
        });
    }

    private AuthResult issueAuthResult(UserEntity user, UUID familyId, String userAgent, String ipAddress) {
        CreatedRefreshToken refreshToken = createRefreshToken(user, familyId, userAgent, ipAddress);
        return new AuthResult(toAuthResponse(user), new RefreshSession(refreshToken.rawToken(), issueCsrfToken()));
    }

    private CreatedRefreshToken createRefreshToken(UserEntity user, UUID familyId, String userAgent, String ipAddress) {
        String rawToken = refreshTokenHashService.generateRawToken();
        RefreshTokenEntity token = new RefreshTokenEntity();
        token.setUser(user);
        token.setTokenHash(refreshTokenHashService.hash(rawToken));
        token.setFamilyId(familyId);
        token.setExpiresAt(Instant.now().plus(properties.refreshTokenTtlDays(), ChronoUnit.DAYS));
        token.setUserAgent(userAgent);
        token.setIpAddress(ipAddress);
        return new CreatedRefreshToken(rawToken, refreshTokenRepository.save(token));
    }

    private AuthResponse toAuthResponse(UserEntity user) {
        CurrentUser currentUser = new CurrentUser(user.getId(), user.getEmail(), user.getDisplayName(), List.of(user.getRole()));
        return new AuthResponse(UserResponse.from(user), jwtTokenService.issue(currentUser));
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

    private record CreatedRefreshToken(String rawToken, RefreshTokenEntity entity) {
    }
}
