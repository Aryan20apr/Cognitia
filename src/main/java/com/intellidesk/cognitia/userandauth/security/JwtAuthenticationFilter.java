package com.intellidesk.cognitia.userandauth.security;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.intellidesk.cognitia.userandauth.multiteancy.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtTokenProvider jwtProvider, UserDetailsService userDetailsService, AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtTokenProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    // @Override
    // protected void doFilterInternal(HttpServletRequest request,
    // HttpServletResponse response, FilterChain filterChain)
    // throws ServletException, IOException {

    // String token = extractTokenFromRequest(request);

    // if (token != null) {
    // try {
    // Jws<Claims> jws = jwtProvider.parseToken(token);
    // Claims claims = jws.getPayload();

    // // Extract user information
    // String userId = claims.getSubject();
    // @SuppressWarnings("unchecked")
    // List<String> roles = claims.get("roles", List.class);
    // String tenantId = claims.get("tenant", String.class);

    // // Set tenant context
    // if (tenantId != null) {
    // TenantContext.setTenantId(UUID.fromString(tenantId));
    // }

    // // Create authorities
    // List<SimpleGrantedAuthority> authorities = roles != null ?
    // roles.stream()
    // .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
    // .toList() : List.of();

    // // Create authentication
    // UsernamePasswordAuthenticationToken auth =
    // new UsernamePasswordAuthenticationToken(userId, null, authorities);

    // SecurityContextHolder.getContext().setAuthentication(auth);

    // } catch (Exception e) {
    // // Token is invalid, continue without authentication
    // logger.debug("Invalid JWT token", e);
    // }
    // }

    // try {
    // filterChain.doFilter(request, response);
    // } finally {
    // TenantContext.clear();
    // }
    // }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (StringUtils.hasText(token)){
              try {
                jwtTokenProvider.validateTokenOrThrow(token); 
                    String email = jwtTokenProvider.getEmailFromToken(token);
    
                    var userDetails = userDetailsService.loadUserByUsername(email);
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
    
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    String tenantId = jwtTokenProvider.getTenantFromToken(token);
                    if (tenantId != null) {
                        log.info("[JwtAuthenticationFilter] [doFilterInternal] Setting tenant context for tenantId: {}", tenantId);
                        TenantContext.setTenantId(UUID.fromString(tenantId));
                    }
              } catch (AuthenticationException e) {
                throw e;
              }
            }

            filterChain.doFilter(request, response);
        } catch (AuthenticationException e) {
            // Log authentication errors and propagate
            log.warn("[JwtAuthenticationFilter] [doFilterInternal] Authentication exception", e);
            authenticationEntryPoint.commence(request, response, e);
            return;
        } catch (Exception e) {
            // Log other errors but don't clear tenant context here - let JwtTenantFilter handle it
            log.error("[JwtAuthenticationFilter] [doFilterInternal] Unexpected exception", e);
            throw new ServletException("JwtAuthenticationFilter: Unexpected error", e);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth") 
            || path.startsWith("/public") 
            || path.startsWith("/api/tenants/create")
            || path.equals("/api/v1/payment/webhook"); // Webhook uses signature verification, not JWT
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
