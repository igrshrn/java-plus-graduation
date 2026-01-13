package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.category.CategoryClient;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/internal/categories")
@RequiredArgsConstructor
public class InternalCategoryController implements CategoryClient {
    private final CategoryService categoryService;

    @Override
    @GetMapping("/{id}")
    public CategoryDto getCategoryById(@PathVariable Long id) {
        return categoryService.getById(id);
    }

    @Override
    @PostMapping("/batch")
    public List<CategoryDto> getCategoriesByIds(@RequestBody List<Long> ids) {
        return categoryService.getCategoriesByIds(ids);
    }
}
