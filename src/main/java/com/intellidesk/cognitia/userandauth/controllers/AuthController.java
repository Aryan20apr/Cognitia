package com.intellidesk.cognitia.userandauth.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intellidesk.cognitia.ingestion.models.dtos.ApiResponse;
import com.intellidesk.cognitia.userandauth.models.dtos.LoginRequestDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.LoginResponseDTO;
import com.intellidesk.cognitia.userandauth.models.dtos.TokenPair;
import com.intellidesk.cognitia.userandauth.models.dtos.UserDetailsDTO;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.repository.UserRepository;
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
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtTokenProvider jwtProvider;
    private final RefreshTokenService refreshService;

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
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO req, HttpServletResponse response) {
        Authentication auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        User user = ((CustomUserDetails) auth.getPrincipal()).getUser();

        String access = jwtProvider.createAccessToken(user);
        String rawRefresh = refreshService.createRefreshToken(user, req.getDeviceId(), req.getIp(), req.getUserAgent());
        setRefreshCookie(response, rawRefresh);
        LoginResponseDTO loginResponseDTO = new LoginResponseDTO();
        loginResponseDTO.setAccessToken(access);
        UserDetailsDTO userDetailsDTO = Utils.mapToUserDetailsDTO(user);
        loginResponseDTO.setUserDetailsDTO(userDetailsDTO);
        return ResponseEntity.ok(loginResponseDTO);
    }

     @Operation(
        summary = "Refresh token",
        description = "Get new access token using refresh token"
    )
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest req, HttpServletResponse res) {
        String raw = readRefreshFromCookieOrBody(req);
        TokenPair pair = refreshService.rotate(raw);
        setRefreshCookie(res, pair.getNewRefreshToken());
        return ResponseEntity.ok(Map.of("accessToken", pair.getNewAccessToken()));
    }

     @Operation(
        summary = "Logout",
        description = "Invalidate refresh token and clear cookie"
    )
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest req, HttpServletResponse res) {
        String raw = readRefreshFromCookieOrBody(req);
        if (raw != null) {
            try { refreshService.revokeByRaw(raw); } catch (Exception ignored) {}
        }
        
        // Clear the refresh token cookie
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Expire immediately
        res.addCookie(cookie);
        
        ApiResponse<HashMap<String,Object>> apiResponse = new ApiResponse<>("Logout successful", false, new HashMap<>());
        return ResponseEntity.ok(apiResponse);
    }



    private void setRefreshCookie(HttpServletResponse response, String refreshToken){
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Use HTTPS in production
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days in seconds
        response.addCookie(cookie);
    }

    private String readRefreshFromCookieOrBody(HttpServletRequest servletRequest){
        // First try to read from cookie
        Cookie[] cookies = servletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
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

