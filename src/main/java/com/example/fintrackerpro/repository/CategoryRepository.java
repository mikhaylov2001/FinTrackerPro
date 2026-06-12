package com.example.fintrackerpro.repository;

import com.example.fintrackerpro.entity.category.Category;
import com.example.fintrackerpro.entity.category.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdAndTypeOrderByNameAsc(Long userId, CategoryType type);

    boolean existsByUserIdAndType(Long userId, CategoryType type);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    Optional<Category> findByUserIdAndTypeAndNameIgnoreCase(Long userId, CategoryType type, String name);
}
