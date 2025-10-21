package com.intellidesk.cognitia.userandauth.security;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.intellidesk.cognitia.userandauth.models.entities.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessTokenMs = Duration.ofMinutes(60).toMillis();
    private final String issuer = "my-monolith";

    public JwtTokenProvider(@Value("${jwt.private-key.path}") String privKeyPath,
                            @Value("${jwt.public-key.path}") String pubKeyPath) {
        this.privateKey = PemUtils.loadPrivateKey(privKeyPath);
        this.publicKey = PemUtils.loadPublicKey(pubKeyPath);
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of(user.getRole().getRoleId())); // or role names
        claims.put("tenant", user.getTenantId());
        claims.put("email", user.getEmail()); // Add email to claims

        return Jwts.builder()
                .id(jti)
                .issuer(issuer)
                .subject(user.getId().toString())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenMs)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Jws<Claims> parseToken(String token) {
        return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Validates the JWT token by parsing it and checking expiration
     * @param token the JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extracts email from JWT token
     * @param token the JWT token
     * @return email from token claims
     */
    public String getEmailFromToken(String token) {
        try {
            Jws<Claims> jws = parseToken(token);
            return jws.getPayload().get("email", String.class);
        } catch (Exception e) {
            logger.error("Error extracting email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts tenant ID from JWT token
     * @param token the JWT token
     * @return tenant ID from token claims
     */
    public String getTenantFromToken(String token) {
        try {
            Jws<Claims> jws = parseToken(token);
            return jws.getPayload().get("tenant", String.class);
        } catch (Exception e) {
            logger.error("Error extracting tenant from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts user ID from JWT token
     * @param token the JWT token
     * @return user ID from token subject
     */
    public String getUserIdFromToken(String token) {
        try {
            Jws<Claims> jws = parseToken(token);
            return jws.getPayload().getSubject();
        } catch (Exception e) {
            logger.error("Error extracting user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts roles from JWT token
     * @param token the JWT token
     * @return list of roles from token claims
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Jws<Claims> jws = parseToken(token);
            return jws.getPayload().get("roles", List.class);
        } catch (Exception e) {
            logger.error("Error extracting roles from token: {}", e.getMessage());
            return null;
        }
    }
}

