package com.articleshelf.application.auth;

import com.articleshelf.domain.user.PasswordPolicy;
import com.articleshelf.domain.user.UserStatus;
import com.articleshelf.domain.user.UsernamePolicy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class InitialUserProvisioner {
    private final AuthUserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AuthSettings settings;
    private final UsernamePolicy usernamePolicy = new UsernamePolicy();
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    public InitialUserProvisioner(
            AuthUserRepository userRepository,
            PasswordHasher passwordHasher,
            AuthSettings settings
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.settings = settings;
    }

    public Optional<AuthUser> ensureInitialUser() {
        if (!settings.initialUserEnabled()) {
            return Optional.empty();
        }
        String username = usernamePolicy.normalize(settings.initialUsername());
        usernamePolicy.validate(username);
        return Optional.of(userRepository.findByUsername(username).orElseGet(() -> {
            passwordPolicy.validate(username, settings.initialUserPassword());
            AuthUser user = new AuthUser(
                    null,
                    username,
                    passwordHasher.hash(settings.initialUserPassword()),
                    "ArticleShelf Owner",
                    "ADMIN",
                    UserStatus.ACTIVE,
                    null,
                    Instant.EPOCH
            );
            return userRepository.save(user);
        }));
    }
}
