package com.articleshelf.infrastructure.security;

import com.articleshelf.application.auth.CurrentUser;
import com.articleshelf.application.auth.AuthUserRepository;
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private final AuthUserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, AuthUserRepository userRepository) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                CurrentUser parsedUser = jwtTokenService.parse(header.substring("Bearer ".length()));
                CurrentUser currentUser = userRepository.findById(parsedUser.id())
                        .filter(user -> user.status() == UserStatus.ACTIVE)
                        .filter(user -> parsedUser.tokenIssuedAt() != null && !parsedUser.tokenIssuedAt().isBefore(user.tokenValidAfter()))
                        .map(user -> new CurrentUser(user.id(), user.username(), user.displayName(), java.util.List.of(user.role()), parsedUser.tokenIssuedAt()))
                        .orElseThrow(() -> new JwtValidationException("invalid token"));
                var authorities = currentUser.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();
                var authentication = new UsernamePasswordAuthenticationToken(currentUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
