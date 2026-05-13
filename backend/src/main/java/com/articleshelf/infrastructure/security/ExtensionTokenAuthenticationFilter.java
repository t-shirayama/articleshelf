package com.articleshelf.infrastructure.security;

import com.articleshelf.application.auth.AuthUserRepository;
import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.application.extension.ExtensionAccessTokenRepository;
import com.articleshelf.application.extension.ExtensionAuthService;
import com.articleshelf.application.observability.BackendMetrics;
import com.articleshelf.domain.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;

@Component
public class ExtensionTokenAuthenticationFilter extends OncePerRequestFilter {
    private final ExtensionAccessTokenRepository tokenRepository;
    private final ExtensionAuthService extensionAuthService;
    private final AuthUserRepository userRepository;
    private final BackendMetrics metrics;
    private final Clock clock;

    public ExtensionTokenAuthenticationFilter(
            ExtensionAccessTokenRepository tokenRepository,
            ExtensionAuthService extensionAuthService,
            AuthUserRepository userRepository,
            BackendMetrics metrics,
            Clock clock
    ) {
        this.tokenRepository = tokenRepository;
        this.extensionAuthService = extensionAuthService;
        this.userRepository = userRepository;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/extension/articles");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authenticate(header.substring("Bearer ".length()));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String rawToken) {
        tokenRepository.findByTokenHash(extensionAuthService.tokenHash(rawToken))
                .filter(token -> token.isActiveAt(clock.instant()))
                .flatMap(token -> userRepository.findById(token.userId())
                        .filter(user -> user.status() == UserStatus.ACTIVE)
                        .filter(user -> !token.createdAt().isBefore(user.tokenValidAfter()))
                        .map(user -> new AuthenticatedExtension(user.id(), user.username(), user.displayName(), user.role(), token.scopeSet())))
                .ifPresentOrElse(this::setAuthentication, () -> metrics.recordAccessTokenRejected("extension_invalid"));
    }

    private void setAuthentication(AuthenticatedExtension extension) {
        CurrentUser user = new CurrentUser(extension.id(), extension.username(), extension.displayName(), java.util.List.of(extension.role()));
        var authorities = extension.scopes().stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
    }

    private record AuthenticatedExtension(
            java.util.UUID id,
            String username,
            String displayName,
            String role,
            java.util.Set<String> scopes
    ) {
    }
}
