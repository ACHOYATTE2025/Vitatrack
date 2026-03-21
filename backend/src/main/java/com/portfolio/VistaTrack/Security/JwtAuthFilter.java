package com.portfolio.VistaTrack.Security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.portfolio.VistaTrack.Services.AuthService;
import com.portfolio.VistaTrack.Services.JwtService;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
      private final JwtService jwtService;

    /**
     * Core filter logic — extracts, validates, and applies the JWT token.
     *
     * @param request     incoming HTTP request
     * @param response    outgoing HTTP response
     * @param filterChain the remaining filter chain
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // ── 1. Read the Authorization header ──────────────────────────────────
        String authHeader = request.getHeader("Authorization");

        // If no Bearer token is present, skip this filter entirely
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("[JWT FILTER] No Bearer token found — skipping authentication");
            filterChain.doFilter(request, response);
            return;
        }

        // ── 2. Extract the raw token (strip "Bearer " prefix) ─────────────────
        String token = authHeader.substring(7);

        // ── 3. Validate the token ──────────────────────────────────────────────
        if (!jwtService.isTokenValid(token)) {
            log.warn("[JWT FILTER] Invalid or expired token — rejecting request to: {}",
                    request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // ── 4. Extract user identity from the token ────────────────────────────
        String userId = jwtService.extractUserId(token);
        String email  = jwtService.extractEmail(token);
        String role   = jwtService.extractRole(token);

        log.info("[JWT FILTER] Token valid — userId: {} | path: {}",
                userId, request.getRequestURI());

        // ── 5. Build Spring Security authentication object ────────────────────
        // The role stored in the token (e.g. "USER") is prefixed with "ROLE_"
        // as required by Spring Security's authority convention
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,                                              // principal (who)
                        null,                                                // credentials (not needed post-auth)
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)) // authorities
                );

        // Attach request metadata to the authentication (IP, session, etc.)
        authentication.setDetails(email);

        // ── 6. Store in SecurityContext so controllers can access it ──────────
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("[JWT FILTER] SecurityContext populated for userId: {}", userId);

        // ── 7. Continue to the next filter / controller ───────────────────────
        filterChain.doFilter(request, response);
    }
    

}
