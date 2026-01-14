package ru.practicum.service;


import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.entity.Category;

import java.util.List;

public interface CategoryService {

    CategoryDto create(NewCategoryDto categoryDto);

    CategoryDto update(NewCategoryDto categoryDto, Long categoryId);

    void delete(Long categoryId);

    List<CategoryDto> getAll(Integer from, Integer size);

    CategoryDto getById(Long categoryId);

    Category getCategoryById(Long categoryId);

    List<CategoryDto> getCategoriesByIds(List<Long> ids);
}
