package com.intellidesk.cognitia.userandauth.services.impl;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String OTP_EMAIL_SUBJECT = "Your Cognitia verification code";
    private static final String TEMPLATE_OTP = "otp";

    @Override
    @Transactional
    public Boolean verifyOtp(String email, String otp) {
        boolean verified = otpService.verify(email, otp);

        if (verified) {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ApiException("User not found"));
            user.setEmailVerified(true);
            userRepository.save(user);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void resendOtp(String email) {
        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }

        String otp = otpService.generateAndStore(email);
        emailService.sendHtml(email, OTP_EMAIL_SUBJECT,
                TEMPLATE_OTP, Map.of("otp", otp, "subject", OTP_EMAIL_SUBJECT));
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
            String otp = otpService.generateAndStore(email);
            emailService.sendHtml(email, "Reset your Cognitia password",
                    TEMPLATE_OTP, Map.of("otp", otp, "subject", "Reset your Cognitia password"));
        }
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        boolean verified = otpService.verify(email, otp);
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

    @Override
    public String validateInviteToken(String token) {
        String email = otpService.peekActivationToken(token);
        if (email == null) {
            throw new ApiException("Invalid or expired invitation link");
        }
        return email;
    }

    @Override
    @Transactional
    public void acceptInvite(String token, String newPassword) {
        String email = otpService.verifyActivationToken(token);
        if (email == null) {
            throw new ApiException("Invalid or expired invitation link");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new ApiException("Account is already activated");
        }

        user.setEmailVerified(true);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("[AuthServiceImpl] [acceptInvite] Invite accepted for {}", email);
    }
}
