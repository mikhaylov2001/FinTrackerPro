package com.example.fintrackerpro.repository;

import com.example.fintrackerpro.entity.auth.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {
    Optional<RefreshTokenEntity> findByIdAndRevokedAtIsNull(String id);
}
