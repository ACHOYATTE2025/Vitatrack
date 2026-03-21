package com.portfolio.VistaTrack.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.portfolio.VistaTrack.Security.JwtAuthFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {
  private final JwtAuthFilter jwtAuthFilter;

    /**
     * Configures the HTTP security filter chain.
     *
     * <p>Rules applied:
     * <ul>
     *   <li>CSRF disabled — not needed for stateless REST APIs</li>
     *   <li>Sessions disabled — each request must carry its own JWT</li>
     *   <li>/register and /login are public — no token required</li>
     *   <li>All other routes require a valid JWT token</li>
     *   <li>JwtAuthFilter runs before Spring's default auth filter</li>
     * </ul>
     * </p>
     *
     * @param http the HttpSecurity builder provided by Spring
     * @return the configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("[SECURITY] Configuring security filter chain");

        http
            // Disable CSRF — irrelevant for stateless JWT-based APIs
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session — Spring will never create or use an HTTP session
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Route authorization rules
            .authorizeHttpRequests(auth -> auth

                // Public routes — no token required
                .requestMatchers(
                    "auth/register",
                    "auth/login",
                    "/actuator/**",      // health check endpoint for Railway
                      "/v3/**", "/swagger-ui/**"//permettre l'affichage de swagger
                ).permitAll()

                // All other routes require an authenticated JWT
                .anyRequest().authenticated()
            )

            // Register the JWT filter before Spring's default username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("[SECURITY] Security filter chain configured successfully");
        return http.build();
    }

   
   

    /**
     * Exposes the AuthenticationManager bean for use in AuthService during login.
     * Required to manually trigger authentication when verifying credentials.
     *
     * @param config Spring's AuthenticationConfiguration
     * @return the application's AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}

