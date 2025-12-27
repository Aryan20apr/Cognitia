package com.intellidesk.cognitia.userandauth.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.common.Constants;
import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.userandauth.models.dtos.LoginRequestDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.LoginResponseDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.TokenPair;
import com.intellidesk.cognitia.userandauth.models.dtos.UserDetailsDTO;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.security.CustomUserDetails;
import com.intellidesk.cognitia.userandauth.security.JwtTokenProvider;
import com.intellidesk.cognitia.userandauth.security.RefreshTokenService;
import com.intellidesk.cognitia.utils.Utils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtTokenProvider jwtProvider;
    private final RefreshTokenService refreshService;
    
    @Value("${cookie.secure:true}")
    private boolean cookieSecure;
    
    @Value("${cookie.sameSite:None}")
    private String cookieSameSite;

     @Operation(
        summary = "Login user",
        description = "Authenticate user and return JWT token"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Authentication successful",
        content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))
    )
     @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "Invalid credentials"
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO req) {
        Authentication auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        User user = ((CustomUserDetails) auth.getPrincipal()).getUser();

        String access = jwtProvider.createAccessToken(user);
        String rawRefresh = refreshService.createRefreshToken(user, req.getDeviceId(), req.getIp(), req.getUserAgent());
        
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO();
        loginResponseDTO.setAccessToken(access);
        loginResponseDTO.setExpiresAt(jwtProvider.getExpirationTimeFromToken(access));
        UserDetailsDTO userDetailsDTO = Utils.mapToUserDetailsDTO(user);
        loginResponseDTO.setUserDetailsDTO(userDetailsDTO);
        
        ResponseCookie cookie = createRefreshCookie(rawRefresh);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(loginResponseDTO);
    }

     @Operation(
        summary = "Refresh token",
        description = "Get new access token using refresh token"
    )
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest req) {
        String raw = readRefreshFromCookieOrBody(req);
        TokenPair pair = refreshService.rotate(raw);
        ResponseCookie cookie = createRefreshCookie(pair.getNewRefreshToken());
        Long expiresAt = jwtProvider.getExpirationTimeFromToken(pair.getNewAccessToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of(
                    "accessToken", pair.getNewAccessToken(),
                    "expiresAt", expiresAt != null ? expiresAt : 0
                ));
    }

     @Operation(
        summary = "Logout",
        description = "Invalidate refresh token and clear cookie"
    )
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest req) {
        String raw = readRefreshFromCookieOrBody(req);

        log.info("[Logout] Attempting logout for refresh token: {}", raw != null ? "[HIDDEN]" : "null");

        if (raw != null) {
            try {
                refreshService.revokeByRaw(raw);
                log.info("[Logout] Refresh token revoked successfully.");
            } catch (Exception e) {
                log.warn("[Logout] Exception while revoking refresh token: {}", e.getMessage());
            }
        } else {
            log.warn("[Logout] No refresh token found in cookie or body for logout.");
        }
        
        // Clear the refresh token cookie
        ResponseCookie cookie = createRefreshCookie("", 0);
        log.info("[Logout] Refresh token cookie cleared.");

        ApiResponse<HashMap<String,Object>> apiResponse = new ApiResponse<>("Logout successful", false, new HashMap<>());
        log.info("[Logout] Responding with logout success.");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(apiResponse);
    }



    private ResponseCookie createRefreshCookie(String refreshToken, int maxAge) {
        // SameSite=None requires Secure=true
        boolean secure = cookieSecure;
        if ("None".equalsIgnoreCase(cookieSameSite) && !cookieSecure) {
            log.warn("[createRefreshCookie] SameSite=None requires Secure=true. Setting Secure=true.");
            secure = true;
        }
        
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secure)
                .path("/auth")
                .maxAge(maxAge);
        
        // Set SameSite attribute based on configuration
        // Using string value as Spring may not expose the enum directly

        switch (cookieSameSite) {
            case Constants.SAME_SITE_NONE -> builder.sameSite(Constants.SAME_SITE_NONE);
            case Constants.SAME_SITE_LAX -> builder.sameSite(Constants.SAME_SITE_LAX);
            case Constants.SAME_SITE_STRICT -> builder.sameSite(Constants.SAME_SITE_STRICT);
        }
        
        ResponseCookie cookie = builder.build();
        log.debug("[createRefreshCookie] Created cookie with Secure={}, SameSite={}, MaxAge={}", 
                secure, cookieSameSite, maxAge);
        return cookie;
    }
    
    private ResponseCookie createRefreshCookie(String refreshToken) {
        return createRefreshCookie(refreshToken, 7 * 24 * 60 * 60); // 7 days in seconds
    }

    private String readRefreshFromCookieOrBody(HttpServletRequest servletRequest){
        // First try to read from cookie
        Cookie[] cookies = servletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.info("[readRefreshFromCookieOrBody] Cookie name: {}, Cookie value: {}", cookie.getName(), cookie.getValue());
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        // If not found in cookie, try to read from request body
        // This would require parsing the request body, which is more complex
        // For now, return null if not found in cookies
        return null;
    }
}

