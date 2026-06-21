package com.example.fintrackerpro.service;

import com.example.fintrackerpro.dto.CategoryRequest;
import com.example.fintrackerpro.dto.CategoryResponse;
import com.example.fintrackerpro.entity.category.Category;
import com.example.fintrackerpro.entity.category.CategoryType;
import com.example.fintrackerpro.entity.user.User;
import com.example.fintrackerpro.exception.ResourceNotFoundException;
import com.example.fintrackerpro.repository.CategoryRepository;
import com.example.fintrackerpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final List<String> DEFAULT_INCOME = List.of(
            "Работа",
            "Подработка",
            "Бизнес",
            "Рента",
            "Инвестиции",
            "Пассивный доход",
            "Вклады",
            "Продажа вещей",
            "Подарки",
            "Другое"
    );

    private static final List<String> DEFAULT_EXPENSE = List.of(
            "Продукты",
            "Коммунальные услуги",
            "Аренда",
            "Транспорт",
            "Фитнес",
            "Здоровье",
            "Подписка на ИИ",
            "Образование",
            "Ипотека",
            "Кредит",
            "Рестораны",
            "Дом",
            "Кафе",
            "Налоги",
            "Развлечения",
            "Другое"
    );

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Long userId, CategoryType type) {
        ensureDefaults(userId, type);
        List<Category> categories = categoryRepository.findByUserIdAndTypeOrderByNameAsc(userId, type);
        return sortInDefaultOrder(categories, type);
    }

    @Transactional
    public CategoryResponse createCategory(Long userId, CategoryRequest request) {
        String name = normalizeName(request.getName());
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название категории обязательно");
        }

        categoryRepository.findByUserIdAndTypeAndNameIgnoreCase(userId, request.getType(), name)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Категория уже существует");
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Category category = Category.builder()
                .user(user)
                .name(name)
                .type(request.getType())
                .system(false)
                .build();

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Категория не найдена"));
        categoryRepository.delete(category);
    }

    @Transactional
    public void seedDefaultsForUser(Long userId) {
        ensureDefaults(userId, CategoryType.INCOME);
        ensureDefaults(userId, CategoryType.EXPENSE);
    }

    @Transactional
    public void ensureDefaults(Long userId, CategoryType type) {
        if (categoryRepository.existsByUserIdAndType(userId, type)) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        List<String> defaults = type == CategoryType.INCOME ? DEFAULT_INCOME : DEFAULT_EXPENSE;

        for (String name : defaults) {
            categoryRepository.save(Category.builder()
                    .user(user)
                    .name(name)
                    .type(type)
                    .system(true)
                    .build());
        }
    }

    private String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    private List<CategoryResponse> sortInDefaultOrder(List<Category> categories, CategoryType type) {
        List<String> defaultOrder = type == CategoryType.INCOME ? DEFAULT_INCOME : DEFAULT_EXPENSE;
        Map<String, Category> byLowerName = categories.stream()
                .collect(Collectors.toMap(
                        c -> c.getName().toLowerCase(Locale.ROOT),
                        c -> c,
                        (a, b) -> a
                ));

        List<CategoryResponse> result = new ArrayList<>();
        Set<String> used = new HashSet<>();

        for (String name : defaultOrder) {
            Category category = findCategoryForDefault(byLowerName, name, type);
            if (category != null) {
                result.add(CategoryResponse.from(category));
                used.add(category.getName().toLowerCase(Locale.ROOT));
            }
        }

        categories.stream()
                .filter(c -> !used.contains(c.getName().toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(Category::getCreatedAt))
                .map(CategoryResponse::from)
                .forEach(result::add);

        return result;
    }

    private Category findCategoryForDefault(Map<String, Category> byLowerName, String name, CategoryType type) {
        Category category = byLowerName.get(name.toLowerCase(Locale.ROOT));
        if (category != null) {
            return category;
        }
        if (type == CategoryType.INCOME && "рента".equals(name.toLowerCase(Locale.ROOT))) {
            return byLowerName.get("аренда недвижимости");
        }
        return null;
    }
}
