package com.railway.main_service.config;

import com.railway.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Spring Security configuration for main-service.
 *
 * DIFFERENCE FROM auth-service:
 *   auth-service has PUBLIC endpoints (/api/auth/**) for login/register.
 *   main-service has NO public endpoints — every single request
 *   must carry a valid JWT. If no token or invalid token → 401.
 *
 * WHAT'S SHARED WITH auth-service (via common-lib):
 *   - JwtAuthenticationFilter: validates Bearer token on every request
 *   - JwtUtil: parses and verifies the token
 *   - AuthPrincipal: the authenticated user/admin object
 *   - TokenBlacklistService: checks if token was invalidated
 *
 * WHY NO PasswordEncoder bean here?
 *   main-service never hashes or verifies passwords.
 *   That's auth-service's responsibility. Adding PasswordEncoder
 *   here would be dead code — misleading and wasteful.
 *
 * WHY @EnableMethodSecurity?
 *   Enables @PreAuthorize on controller methods. Example:
 *     @PreAuthorize("hasRole('USER')")      → only users
 *     @PreAuthorize("hasRole('ADMIN')")     → only admins
 *     @PreAuthorize("isAuthenticated()")    → any authenticated
 *   Without this annotation, @PreAuthorize is silently ignored.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http
      // Disable Spring Security's CORS — handled by our
      // standalone CorsFilter at highest precedence (see below).
      // Reason: Security CORS runs inside security chain.
      // If request is rejected (401) before CORS headers are added,
      // browser reports "CORS error" instead of the real 401.
      .cors(AbstractHttpConfigurer::disable)

      // Disable CSRF — stateless REST API using JWT.
      // CSRF attacks target session-based apps where browsers
      // automatically send session cookies. We don't use sessions.
      .csrf(AbstractHttpConfigurer::disable)

      // Stateless — no server-side sessions, ever.
      // Every request authenticates independently via JWT.
      // This is the only correct mode for microservices.
      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )

      .authorizeHttpRequests(auth -> auth

        // Actuator health endpoint — public so load balancers
        // and monitoring tools can check health without a token.
        // Only expose health — not metrics, not env, not beans.
        .requestMatchers("/actuator/health").permitAll()

        // Everything else requires a valid JWT.
        // No public endpoints in main-service — unlike auth-service
        // which needs /api/auth/** open for login/register.
        .anyRequest().authenticated()
      )

      // Add JWT filter BEFORE Spring's default auth filter.
      // WHY before? Spring's default filter would try username/password
      // auth, which we don't use. Our filter runs first, sets the
      // SecurityContext from JWT, then Spring's auth layer just
      // checks if SecurityContext is populated.
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Standalone CORS filter at HIGHEST_PRECEDENCE.
   *
   * Runs BEFORE the entire Spring Security filter chain.
   * This ensures CORS headers are always present — even on
   * 401/403 responses. Without this, the browser shows
   * "CORS error" instead of the real error, making debugging hell.
   *
   * ALLOWED ORIGINS: same as auth-service.
   * All your frontend domains — local dev + deployed environments.
   * Add new origins here when you add new frontend deployments.
   */
  @Bean
  public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(
      "http://localhost:3000",
      "http://localhost:5173",
      "https://railtick.in",
      "https://admin.railtick.in",
      "https://dev.railtick.in",
      "https://dev.admin.railtick.in"
    ));
    config.setAllowedMethods(List.of(
      "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    ));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    FilterRegistrationBean<CorsFilter> bean =
      new FilterRegistrationBean<>(new CorsFilter(source));
    bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return bean;
  }
}
