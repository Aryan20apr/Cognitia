package com.intellidesk.cognitia.userandauth.services.impl;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.common.Constants;
import com.intellidesk.cognitia.notification.EmailService;
import com.intellidesk.cognitia.notification.OtpService;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.repository.UserRepository;
import com.intellidesk.cognitia.userandauth.security.RefreshTokenService;
import com.intellidesk.cognitia.userandauth.services.AuthService;
import com.intellidesk.cognitia.utils.exceptionHandling.exceptions.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public void verifySignupOtp(String email, String otp) {
        boolean verified = otpService.verify(email, Constants.OTP_PURPOSE_SIGNUP, otp);
        if (!verified) {
            throw new ApiException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Override
    public void resendOtp(String email, String purpose) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }

        String otp = otpService.generateAndStore(email, purpose);
        emailService.sendHtml(email, "Your Cognitia verification code",
                Constants.TEMPLATE_OTP, Map.of("otp", otp, "subject", "Your Cognitia verification code"));
    }

    @Override
    @Transactional
    public void activateAccount(String token) {
        String email = otpService.verifyActivationToken(token);
        if (email == null) {
            throw new ApiException("Invalid or expired activation link");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Override
    public void forgotPassword(String email) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            String otp = otpService.generateAndStore(email, Constants.OTP_PURPOSE_RESET);
            emailService.sendHtml(email, "Reset your Cognitia password",
                    Constants.TEMPLATE_OTP, Map.of("otp", otp, "subject", "Reset your Cognitia password"));
        }
        // Silent return for both cases to prevent email enumeration
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        boolean verified = otpService.verify(email, Constants.OTP_PURPOSE_RESET, otp);
        if (!verified) {
            throw new ApiException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Invalid request"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
    }

    @Override
    public boolean isEmailVerified(String email) {
        return userRepository.findByEmail(email)
                .map(user -> Boolean.TRUE.equals(user.getEmailVerified()))
                .orElse(false);
    }
}
