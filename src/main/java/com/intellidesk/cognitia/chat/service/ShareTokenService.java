package com.intellidesk.cognitia.chat.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ShareTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 16;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final String publicBaseUrl;

    public ShareTokenService(@Value("${cognitia.app.public-base-url}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String generateUniqueToken(TokenValidator validator) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            String token = generateToken();
            try {
                if (validator.isValid(token)) {
                    return token;
                }
            } catch (DataIntegrityViolationException e) {
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    log.error("[ShareTokenService] Failed to generate unique token after {} attempts", MAX_RETRY_ATTEMPTS);
                    throw new RuntimeException("Failed to generate unique token after multiple attempts", e);
                }
                log.warn("[ShareTokenService] Token collision on attempt {}, retrying...", attempt);
            }
        }
        throw new RuntimeException("Failed to generate unique token");
    }

    public String buildUrl(String token) {
        return publicBaseUrl + "/s/" + token;
    }

    @FunctionalInterface
    public interface TokenValidator {
        boolean isValid(String token) throws DataIntegrityViolationException;
    }
}
