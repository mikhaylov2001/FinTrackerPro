package com.example.fintrackerpro.repository;

import com.example.fintrackerpro.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserName(String userName);
    Optional<User> findByEmail(String email);
    Optional<User> findByChatId(Long chatId);


    Object existsByUserName(String s);

    Object existsByEmail(String mail);
}
