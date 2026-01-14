package ru.practicum.client.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;

import java.util.List;

@Slf4j
@Component
public class EventClientFallback implements EventClient {

    @Override
    public EventShortDto getEventById(Long id) {
        log.warn("Event-service недоступен. Возвращаем заглушку для события {}", id);
        return EventShortDto.builder()
                .id(id)
                .annotation("[Event Unavailable]")
                .title("[Event Unavailable]")
                .build();
    }

    @Override
    public EventFullDto getEventFullById(Long id) {
        log.warn("Event-service недоступен. Возвращаем полную заглушку для события {}", id);
        return EventFullDto.builder()
                .id(id)
                .annotation("[Event Unavailable]")
                .title("[Event Unavailable]")
                .build();
    }

    @Override
    public List<EventShortDto> getEventsByIds(List<Long> ids) {
        log.warn("Event-service недоступен. Возвращаем пустой список событий для ID: {}", ids);
        return List.of();
    }

    @Override
    public List<EventFullDto> getEventsFullByIds(List<Long> ids) {
        log.warn("Event-service недоступен. Возвращаем пустой список полных событий для ID: {}", ids);
        return List.of();
    }


    @Override
    public Long countEventsByCategoryId(Long categoryId) {
        log.warn("Event-service недоступен. Возвращаем 0 событий для категории {}", categoryId);
        return 0L;
    }

    @Override
    public void updateConfirmedRequests(Long eventId, Long confirmedRequests) {
        log.warn("Event-service недоступен. Невозможно обновить confirmedRequests для события {} до значения {}", eventId, confirmedRequests);
    }
}
