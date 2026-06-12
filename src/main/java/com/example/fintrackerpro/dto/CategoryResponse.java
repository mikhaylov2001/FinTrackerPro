package com.example.fintrackerpro.dto;

import com.example.fintrackerpro.entity.category.Category;
import com.example.fintrackerpro.entity.category.CategoryType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryResponse {

    private Long id;
    private String name;
    private CategoryType type;
    private boolean system;

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .system(category.isSystem())
                .build();
    }
}
