package com.intellidesk.cognitia.userandauth.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.common.Constants;
import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.notification.OtpService;
import com.intellidesk.cognitia.userandauth.models.dtos.LoginRequestDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.LoginResponseDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.TokenPair;
import com.intellidesk.cognitia.userandauth.models.dtos.UserDetailsDTO;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.security.CustomUserDetails;
import com.intellidesk.cognitia.userandauth.security.JwtTokenProvider;
import com.intellidesk.cognitia.userandauth.security.RefreshTokenService;
import com.intellidesk.cognitia.userandauth.services.AuthService;
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

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final int REFRESH_COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    private final AuthenticationManager authManager;
    private final JwtTokenProvider jwtProvider;
    private final RefreshTokenService refreshService;
    private final AuthService authService;
    private final OtpService otpService;
    
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

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>("Please verify your email before logging in", false, null));
        }

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
        
        ResponseCookie cookie = createRefreshCookie("", 0);
        log.info("[Logout] Refresh token cookie cleared.");

        ApiResponse<HashMap<String,Object>> apiResponse = new ApiResponse<>("Logout successful", false, new HashMap<>());
        log.info("[Logout] Responding with logout success.");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(apiResponse);
    }

    @Operation(summary = "Verify signup OTP", description = "Verify email address using OTP sent during signup")
    @PostMapping("/verify-signup-otp")
    public ResponseEntity<?> verifySignupOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Email and OTP are required", false, null));
        }

        if (otpService.isRateLimited(email, Constants.OTP_PURPOSE_SIGNUP)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiResponse<>("Too many attempts. Please try again later.", false, null));
        }

        authService.verifySignupOtp(email, otp);
        return ResponseEntity.ok(new ApiResponse<>("Email verified successfully", true, null));
    }

    @Operation(summary = "Resend OTP", description = "Resend verification OTP to email")
    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String purpose = body.getOrDefault("purpose", Constants.OTP_PURPOSE_SIGNUP);

        if (email == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Email is required", false, null));
        }

        if (otpService.isRateLimited(email, purpose)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiResponse<>("Too many attempts. Please try again later.", false, null));
        }

        authService.resendOtp(email, purpose);
        return ResponseEntity.ok(new ApiResponse<>("If the email exists, an OTP has been sent.", true, null));
    }

    @Operation(summary = "Activate account via link", description = "Activate account using activation token from email")
    @GetMapping("/activate")
    public ResponseEntity<?> activateAccount(@RequestParam String token) {
        authService.activateAccount(token);
        return ResponseEntity.ok(new ApiResponse<>("Account activated successfully", true, null));
    }

    @Operation(summary = "Forgot password", description = "Send password reset OTP to email")
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Email is required", false, null));
        }

        authService.forgotPassword(email);
        return ResponseEntity.ok(new ApiResponse<>("If the email exists, a reset code has been sent.", true, null));
    }

    @Operation(summary = "Reset password", description = "Reset password using OTP")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        String newPassword = body.get("newPassword");

        if (email == null || otp == null || newPassword == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>("Email, OTP, and new password are required", false, null));
        }

        if (otpService.isRateLimited(email, Constants.OTP_PURPOSE_RESET)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ApiResponse<>("Too many attempts. Please try again later.", false, null));
        }

        authService.resetPassword(email, otp, newPassword);
        return ResponseEntity.ok(new ApiResponse<>("Password reset successfully", true, null));
    }

    private ResponseCookie createRefreshCookie(String refreshToken, int maxAge) {
        boolean secure = cookieSecure;
        if ("None".equalsIgnoreCase(cookieSameSite) && !cookieSecure) {
            log.warn("[createRefreshCookie] SameSite=None requires Secure=true. Setting Secure=true.");
            secure = true;
        }
        
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .path("/auth")
                .maxAge(maxAge);

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
        return createRefreshCookie(refreshToken, REFRESH_COOKIE_MAX_AGE_SECONDS);
    }

    private String readRefreshFromCookieOrBody(HttpServletRequest servletRequest){
        Cookie[] cookies = servletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.info("[readRefreshFromCookieOrBody] Cookie name: {}, Cookie value: {}", cookie.getName(), cookie.getValue());
                if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
