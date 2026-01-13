package ru.practicum.client.category;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.category.CategoryDto;

import java.util.List;

@Slf4j
@Component
public class CategoryClientFallback implements CategoryClient {

    @Override
    public CategoryDto getCategoryById(Long id) {
        log.warn("Category-service недоступен. Возвращаем заглушку для категории {}", id);
        return CategoryDto.builder()
                .id(id)
                .name("[Category Unavailable]")
                .build();
    }

    @Override
    public List<CategoryDto> getCategoriesByIds(List<Long> ids) {
        log.warn("Category-service недоступен. Возвращаем пустой список категорий для ID: {}", ids);
        return List.of();
    }
}
