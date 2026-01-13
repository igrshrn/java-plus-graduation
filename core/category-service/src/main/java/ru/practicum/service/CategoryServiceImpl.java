package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.event.EventClient;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.entity.Category;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.repository.CategoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventClient eventClient;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto create(NewCategoryDto categoryDto) {
        if (categoryRepository.existsByName(categoryDto.getName())) {
            throw new ConflictException("Категория %s уже существует".formatted(categoryDto.getName()));
        }
        Category category = categoryMapper.toCategory(categoryDto);
        return categoryMapper.toCategoryDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryDto update(NewCategoryDto categoryDto, Long categoryId) {
        Category existingCategory = getCategoryById(categoryId);
        if (!existingCategory.getName().equals(categoryDto.getName())) {
            categoryRepository.findByName(categoryDto.getName()).ifPresent(c -> {
                throw new ConflictException("Категория  %s уже существует".formatted(categoryDto.getName()));
            });
        }
        existingCategory.setName(categoryDto.getName());
        return categoryMapper.toCategoryDto(categoryRepository.save(existingCategory));
    }

    @Override
    @Transactional
    public void delete(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Категория id = %d не найдена".formatted(categoryId));
        }

        Long eventCount = eventClient.countEventsByCategoryId(categoryId);
        if (eventCount > 0) {
            throw new ConflictException("Попытка удалить категорию с привязанными событиями");
        }
        categoryRepository.deleteById(categoryId);
    }

    @Override
    public List<CategoryDto> getAll(Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Category> categories = categoryRepository.findAll(pageable).getContent();
        return categories.stream()
                .map(categoryMapper::toCategoryDto)
                .toList();
    }

    @Override
    public CategoryDto getById(Long categoryId) {
        Category category = getCategoryById(categoryId);
        return categoryMapper.toCategoryDto(category);
    }

    @Override
    public Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория id = %d не найдена".formatted(categoryId)));

    }

    @Override
    public List<CategoryDto> getCategoriesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return categoryRepository.findAllById(ids).stream()
                .map(categoryMapper::toCategoryDto)
                .toList();
    }
}
