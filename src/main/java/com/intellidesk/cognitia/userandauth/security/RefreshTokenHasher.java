package com.intellidesk.cognitia.userandauth.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

@Component
public class RefreshTokenHasher {
    private final Mac mac;

    public RefreshTokenHasher(@Value("${refresh.secret}") String secret) throws GeneralSecurityException {
        mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    }

    public String hash(String token) {
        byte[] h = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
        return 
        Hex.encodeHexString(h);
    }
}
