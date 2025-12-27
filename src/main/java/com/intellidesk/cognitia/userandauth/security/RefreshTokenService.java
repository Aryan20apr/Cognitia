package com.intellidesk.cognitia.userandauth.security;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.intellidesk.cognitia.userandauth.models.dtos.TokenPair;
import com.intellidesk.cognitia.userandauth.models.entities.RefreshToken;
import com.intellidesk.cognitia.userandauth.models.entities.User;
import com.intellidesk.cognitia.userandauth.repository.RefreshTokenRepository;
import com.intellidesk.cognitia.utils.exceptionHandling.InvalidTokenException;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RefreshTokenService {


    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository repo;
    private final RefreshTokenHasher hasher;
    private final SecureRandom random = new SecureRandom();
    private final long refreshExpirationMs = Duration.ofDays(14).toMillis();

    public String createRefreshToken(User user, String deviceId, String ip, String ua) {
        log.debug("[createRefreshToken] Creating refresh token for user: {}, deviceId: {}, ip: {}, ua: {}", user != null ? user.getId() : null, deviceId, ip, ua);
        String raw = newTokenValue();
        String hash = hasher.hash(raw);

        RefreshToken r = new RefreshToken();
        r.setTokenHash(hash);
        r.setUser(user);
        r.setCreatedAt(new Date());
        r.setExpiresAt(new Date(System.currentTimeMillis() + refreshExpirationMs));
        r.setRevoked(false);
        r.setUserAgent(ua);
        log.debug("[createRefreshToken] Saving new refresh token entity for user id: {}", user != null ? user.getId() : null);
        repo.save(r);
        log.info("[createRefreshToken] Refresh token created and saved for user id: {}", user != null ? user.getId() : null);
        return raw;
    }

    @Transactional
    public TokenPair rotate(String oldRawToken) {
        log.debug("[rotate] Attempting to rotate refresh token: [PROTECTED]");
        String oldHash = hasher.hash(oldRawToken);
        RefreshToken old = repo.findByTokenHashForUpdate(oldHash)
            .orElseThrow(() -> {
                log.warn("[rotate] Invalid refresh token attempted: [PROTECTED]");
                return new InvalidTokenException("Invalid refresh token");
            });

        if (Boolean.TRUE.equals(old.getRevoked()) || old.getExpiresAt().before(new Date())) {
            log.warn("[rotate] Token reuse or expired detected for user id: {}. Revoking all tokens.", old.getUser().getId());
            // reuse or expired -> revoke all sessions (security)
            revokeAllForUser(old.getUser().getId());
            throw new InvalidTokenException("Refresh token invalid or reused");
        }

        log.debug("[rotate] Marking old token as revoked for token id: {}", old.getId());
        // mark old revoked and create new
        old.setRevoked(true);
        RefreshToken created = new RefreshToken();
        String newRaw = newTokenValue();
        created.setTokenHash(hasher.hash(newRaw));
        created.setUser(old.getUser());
        created.setCreatedAt(new Date());
        created.setExpiresAt(new Date(System.currentTimeMillis() + refreshExpirationMs));
        log.debug("[rotate] Saving new refresh token for user id: {}", old.getUser().getId());
        repo.save(created);

        old.setReplacedBy(created.getId());
        repo.save(old);
        log.info("[rotate] Rotated token successfully for user id: {}. Old token id: {}, New token id: {}", old.getUser().getId(), old.getId(), created.getId());

        String newAccess = jwtTokenProvider.createAccessToken(old.getUser())/* use JwtTokenProvider to create access token for old.getUser() */;
        return new TokenPair(newAccess, newRaw);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        log.debug("[revokeAllForUser] Revoking all tokens for user id: {}", userId);
        List<RefreshToken> tokens = repo.findByUserIdAndRevokedFalse(userId);
        tokens.forEach(t -> t.setRevoked(true));
        repo.saveAll(tokens);
        log.info("[revokeAllForUser] Revoked {} active tokens for user id: {}", tokens.size(), userId);
    }

    private String newTokenValue() {
        byte[] b = new byte[64];
        random.nextBytes(b);
        String tokenValue = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        log.debug("[newTokenValue] Generated new refresh token value (protected)");
        return tokenValue;
    }

    @Transactional
    public Integer revokeByRaw(String rawToken){
        log.debug("[revokeByRaw] Revoking token by raw value (protected, hash will be logged)");
        String tokenHash = hasher.hash(rawToken);
        log.info("[revokeByRaw] tokenHash: "+tokenHash);
        Integer result = repo.revokeToken(tokenHash);
        log.info("[revokeByRaw] Revocation result: {} for tokenHash: {}", result, tokenHash);
        return result;
    }
}
