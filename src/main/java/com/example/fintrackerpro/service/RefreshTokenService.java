package com.example.fintrackerpro.service;

import com.example.fintrackerpro.entity.auth.RefreshTokenEntity;
import com.example.fintrackerpro.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repo;

    public record StoredRefresh(Long userId) {}

    public StoredRefresh validateAndGet(String refreshId, String refreshToken) {
        Optional<RefreshTokenEntity> opt = repo.findById(refreshId);
        if (opt.isEmpty()) {
            return null;
        }

        RefreshTokenEntity entity = opt.get();

        if (!entity.isActive()) {
            return null;
        }

        String incomingHash = sha256(refreshToken);
        if (!incomingHash.equals(entity.getTokenHash())) {
            return null;
        }

        return new StoredRefresh(entity.getUserId());
    }

    public void create(String refreshId, Long userId, String refreshJwt, Instant expiresAt) {
        RefreshTokenEntity e = RefreshTokenEntity.builder()
                .id(refreshId)
                .userId(userId)
                .tokenHash(sha256(refreshJwt))
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build();

        repo.save(e);
    }

    public RefreshTokenEntity requireActive(String id, String refreshJwt) {
        RefreshTokenEntity e = repo.findByIdAndRevokedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (!e.isActive()) throw new IllegalArgumentException("Refresh expired/revoked");
        if (!e.getTokenHash().equals(sha256(refreshJwt))) throw new IllegalArgumentException("Invalid refresh token");
        return e;
    }

    public void revoke(String id) {
        repo.findById(id).ifPresent(e -> {
            e.setRevokedAt(Instant.now());
            repo.save(e);
        });
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
