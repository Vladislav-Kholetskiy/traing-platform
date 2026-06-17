package com.vladislav.training.platform.application.actor.dev;

import com.vladislav.training.platform.userorg.domain.AppUser;
import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Фильтр {@code DevDemoActorAuthenticationFilter}.
 */
class DevDemoActorAuthenticationFilter extends OncePerRequestFilter {

    private final String headerName;
    private final AppUserRepository appUserRepository;

    DevDemoActorAuthenticationFilter(String headerName, AppUserRepository appUserRepository) {
        this.headerName = Objects.requireNonNull(headerName, "headerName must not be null");
        this.appUserRepository = Objects.requireNonNull(appUserRepository, "appUserRepository must not be null");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Long actorUserId = resolveActorUserId(request);
            if (actorUserId != null) {
                DevDemoActorPrincipal principal = new DevDemoActorPrincipal(actorUserId, "demo-actor-" + actorUserId);
                UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(principal, "N/A", List.of());
                authentication.setDetails("dev-demo-auth-bridge");
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private Long resolveActorUserId(HttpServletRequest request) {
        String rawValue = request.getHeader(headerName);
        if (rawValue == null) {
            return null;
        }
        String trimmedValue = rawValue.trim();
        if (trimmedValue.isBlank()) {
            return null;
        }
        try {
            Long actorUserId = Long.parseLong(trimmedValue);
            return actorUserId > 0 ? actorUserId : null;
        } catch (NumberFormatException exception) {
            return resolveActorUserIdByEmployeeNumber(trimmedValue);
        }
    }

    private Long resolveActorUserIdByEmployeeNumber(String employeeNumber) {
        try {
            AppUser user = appUserRepository.findUserByEmployeeNumber(employeeNumber);
            return user.id();
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
