package com.example.fintrackerpro.dto;

import com.example.fintrackerpro.entity.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Название категории обязательно")
    @Size(max = 50, message = "Название категории не длиннее 50 символов")
    private String name;

    @NotNull(message = "Тип категории обязателен")
    private CategoryType type;
}
