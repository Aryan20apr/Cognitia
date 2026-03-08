package com.intellidesk.cognitia.notification;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;

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
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 5;

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

    private String otpKey(String purpose, String email) {
        return "otp:" + purpose + ":" + email;
    }

    private String rateLimitKey(String purpose, String email) {
        return "otp-attempts:" + purpose + ":" + email;
    }
}
