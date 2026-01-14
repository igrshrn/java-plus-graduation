package ru.practicum.client.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.user.UserShortDto;

import java.util.List;

@Slf4j
@Component
public class UserClientFallback implements UserClient {
    @Override
    public UserShortDto getUserById(Long id) {
        log.warn("User-service недоступен. Возвращаем заглушку для пользователя {}", id);
        return UserShortDto.builder()
                .id(id)
                .name("[User Unavailable]")
                .build();
    }

    @Override
    public List<UserShortDto> getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        log.warn("User-service недоступен. Возвращаем заглушки для пользователей: {}", ids);
        return ids.stream()
                .map(id -> UserShortDto.builder()
                        .id(id)
                        .name("[User Unavailable]")
                        .build())
                .toList();
    }
}
