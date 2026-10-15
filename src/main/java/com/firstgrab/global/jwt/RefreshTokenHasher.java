package com.firstgrab.global.jwt;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RefreshTokenHasher {

    private static final String ALGORITHM = "SHA-256";

    public String hash(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashed = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
