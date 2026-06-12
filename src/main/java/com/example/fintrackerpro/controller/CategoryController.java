package com.example.fintrackerpro.controller;

import com.example.fintrackerpro.dto.CategoryRequest;
import com.example.fintrackerpro.dto.CategoryResponse;
import com.example.fintrackerpro.entity.category.CategoryType;
import com.example.fintrackerpro.security.CurrentUser;
import com.example.fintrackerpro.service.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/me")
    public ResponseEntity<List<CategoryResponse>> getMyCategories(
            @RequestParam CategoryType type,
            Authentication auth
    ) {
        Long userId = CurrentUser.id(auth);
        return ResponseEntity.ok(categoryService.getCategories(userId, type));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request,
            Authentication auth
    ) {
        Long userId = CurrentUser.id(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(userId, request));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long categoryId,
            Authentication auth
    ) {
        Long userId = CurrentUser.id(auth);
        categoryService.deleteCategory(userId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
