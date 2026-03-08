package com.intellidesk.cognitia.notification;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration ACTIVATION_TOKEN_TTL = Duration.ofHours(24);
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 5;

    private static final String KEY_PREFIX_OTP = "otp:";
    private static final String KEY_PREFIX_RATE_LIMIT = "otp-attempts:";
    private static final String KEY_PREFIX_ACTIVATION = "activation:";

    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRE_SCRIPT;

    static {
        INCREMENT_WITH_EXPIRE_SCRIPT = new DefaultRedisScript<>();
        INCREMENT_WITH_EXPIRE_SCRIPT.setScriptText(
                "local count = redis.call('INCR', KEYS[1]) " +
                "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
                "return count"
        );
        INCREMENT_WITH_EXPIRE_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateAndStore(String email, String purpose) {
        String otp = generateOtp();
        String key = otpKey(purpose, email);
        stringRedisTemplate.opsForValue().set(key, otp, OTP_TTL);
        log.info("OTP stored for {} with purpose {}", email, purpose);
        return otp;
    }

    public boolean verify(String email, String purpose, String otp) {
        if (!checkRateLimit(email, purpose)) {
            log.warn("Rate limit exceeded for {} with purpose {}", email, purpose);
            return false;
        }

        String key = otpKey(purpose, email);
        String storedOtp = stringRedisTemplate.opsForValue().getAndDelete(key);

        if (storedOtp != null && storedOtp.equals(otp)) {
            deleteRateLimitCounter(email, purpose);
            log.info("OTP verified successfully for {} with purpose {}", email, purpose);
            return true;
        }

        incrementAttempts(email, purpose);
        log.warn("OTP verification failed for {} with purpose {}", email, purpose);
        return false;
    }

    public boolean isRateLimited(String email, String purpose) {
        return !checkRateLimit(email, purpose);
    }

    private boolean checkRateLimit(String email, String purpose) {
        String counterKey = rateLimitKey(purpose, email);
        String attemptsStr = stringRedisTemplate.opsForValue().get(counterKey);
        return attemptsStr == null || Integer.parseInt(attemptsStr) < MAX_ATTEMPTS;
    }

    private void incrementAttempts(String email, String purpose) {
        String counterKey = rateLimitKey(purpose, email);
        long ttlSeconds = RATE_LIMIT_WINDOW.toSeconds();
        stringRedisTemplate.execute(
                INCREMENT_WITH_EXPIRE_SCRIPT,
                List.of(counterKey),
                String.valueOf(ttlSeconds)
        );
    }

    private void deleteRateLimitCounter(String email, String purpose) {
        stringRedisTemplate.delete(rateLimitKey(purpose, email));
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int otp = secureRandom.nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", otp);
    }

    public String generateActivationToken(String email) {
        String token = UUID.randomUUID().toString();
        String key = KEY_PREFIX_ACTIVATION + token;
        stringRedisTemplate.opsForValue().set(key, email, ACTIVATION_TOKEN_TTL);
        log.info("Activation token stored for {}", email);
        return token;
    }

    public String verifyActivationToken(String token) {
        String key = KEY_PREFIX_ACTIVATION + token;
        String email = stringRedisTemplate.opsForValue().getAndDelete(key);
        if (email != null) {
            log.info("Activation token verified for {}", email);
        } else {
            log.warn("Invalid or expired activation token");
        }
        return email;
    }

    private String otpKey(String purpose, String email) {
        return KEY_PREFIX_OTP + purpose + ":" + email;
    }

    private String rateLimitKey(String purpose, String email) {
        return KEY_PREFIX_RATE_LIMIT + purpose + ":" + email;
    }
}
