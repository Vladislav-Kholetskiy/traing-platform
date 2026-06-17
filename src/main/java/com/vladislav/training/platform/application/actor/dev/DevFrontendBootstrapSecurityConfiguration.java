package com.vladislav.training.platform.application.actor.dev;

import com.vladislav.training.platform.userorg.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Конфигурация {@code DevFrontendBootstrapSecurityConfiguration}.
 */
@Configuration(proxyBeanMethods = false)
@Profile("dev")
public class DevFrontendBootstrapSecurityConfiguration {

    @Bean
    DevDemoActorAuthenticationFilter devDemoActorAuthenticationFilter(
        AppUserRepository appUserRepository,
        @Value("${training-platform.dev.demo-auth.header-name:X-Demo-Actor-Id}") String headerName
    ) {
        return new DevDemoActorAuthenticationFilter(headerName, appUserRepository);
    }

    @Bean
    SecurityFilterChain devFrontendBootstrapSecurityFilterChain(
        HttpSecurity http,
        DevDemoActorAuthenticationFilter devDemoActorAuthenticationFilter
    ) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .anonymous(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
            .addFilterBefore(devDemoActorAuthenticationFilter, AnonymousAuthenticationFilter.class)
            .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
        @Value("${training-platform.dev.frontend.allowed-origin:http://localhost:5173,http://127.0.0.1:5173}") String allowedOrigins,
        @Value("${training-platform.dev.demo-auth.header-name:X-Demo-Actor-Id}") String demoActorHeaderName
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
            java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList()
        );
        configuration.setAllowedMethods(java.util.List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(java.util.List.of("Content-Type", "Authorization", demoActorHeaderName));
        configuration.setExposedHeaders(java.util.List.of("X-Correlation-Id", "X-Request-Id"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
