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
        String raw = newTokenValue();
        String hash = hasher.hash(raw);

        RefreshToken r = new RefreshToken();
        r.setTokenHash(hash);
        r.setUser(user);
        r.setCreatedAt(new Date());
        r.setExpiresAt(new Date(System.currentTimeMillis() + refreshExpirationMs));
        r.setRevoked(false);
        r.setUserAgent(ua);
        repo.save(r);
        return raw;
    }

    @Transactional
    public TokenPair rotate(String oldRawToken) {
        String oldHash = hasher.hash(oldRawToken);
        RefreshToken old = repo.findByTokenHashForUpdate(oldHash)
            .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (Boolean.TRUE.equals(old.getRevoked()) || old.getExpiresAt().before(new Date())) {
            // reuse or expired -> revoke all sessions (security)
            revokeAllForUser(old.getUser().getId());
            throw new InvalidTokenException("Refresh token invalid or reused");
        }

        // mark old revoked and create new
        old.setRevoked(true);
        RefreshToken created = new RefreshToken();
        String newRaw = newTokenValue();
        created.setTokenHash(hasher.hash(newRaw));
        created.setUser(old.getUser());
        created.setCreatedAt(new Date());
        created.setExpiresAt(new Date(System.currentTimeMillis() + refreshExpirationMs));
        repo.save(created);

        old.setReplacedBy(created.getId());
        repo.save(old);

        String newAccess = jwtTokenProvider.createAccessToken(old.getUser())/* use JwtTokenProvider to create access token for old.getUser() */;
        return new TokenPair(newAccess, newRaw);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        List<RefreshToken> tokens = repo.findByUserIdAndRevokedFalse(userId);
        tokens.forEach(t -> t.setRevoked(true));
        repo.saveAll(tokens);
    }

    private String newTokenValue() {
        byte[] b = new byte[64];
        random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    @Transactional
    public Integer revokeByRaw(String rawToken){
        String tokenHash = hasher.hash(rawToken);
        log.info("[revokeByRaw] tokenHash: "+tokenHash);
        return repo.revokeToken(tokenHash);
    }
}
