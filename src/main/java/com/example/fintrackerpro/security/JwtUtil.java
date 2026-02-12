package com.example.fintrackerpro.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret-base64:}")
    private String secretBase64;

    @Value("${jwt.access-expiration-ms:900000}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Value("${jwt.issuer:fintracker}")
    private String issuer;

    @Value("${jwt.audience:fintracker-frontend}")
    private String audience;

    public String generateAccessToken(Long userId) {
        return buildToken(userId, "access", accessExpirationMs);
    }

    public String generateRefreshToken(Long userId) {
        return buildToken(userId, "refresh", refreshExpirationMs);
    }

    private String buildToken(Long userId, String type, long expMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expMs)))
                .id(UUID.randomUUID().toString())           // <-- jti
                .claim("typ", type)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Long extractUserId(String token) {
        return Long.parseLong(parse(token).getSubject());
    }

    public String extractType(String token) {
        Object v = parse(token).get("typ");
        return v == null ? null : v.toString();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretBase64);
        return Keys.hmacShaKeyFor(keyBytes); // HS256 min 256-bit key [web:5581]
    }
    public boolean isExpired(String token) {
        Date exp = extractExpiration(token);
        return exp == null || exp.before(new Date());
    }


    public Date extractExpiration(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())   // SecretKey (HMAC) или PublicKey (RSA/ECDSA)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getExpiration();
    }

}
