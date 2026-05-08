package com.articleshelf.domain.user;

import java.util.Locale;
import java.util.regex.Pattern;

public class UsernamePolicy {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]{3,32}$");

    public String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public void validate(String username) {
        String normalized = normalize(username);
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw new UsernamePolicyException("username must be 3 to 32 lowercase letters, numbers, dots, underscores, or hyphens");
        }
    }
}
