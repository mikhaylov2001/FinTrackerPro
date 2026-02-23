package com.example.fintrackerpro.repository;

import com.example.fintrackerpro.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByChatId(Long chatId);


    boolean existsByUserName(String s);

    boolean existsByEmail(String mail);
    Optional<User> findByUserName(String userName);

    Optional<Object> findByEmailIgnoreCase(String email);
}

