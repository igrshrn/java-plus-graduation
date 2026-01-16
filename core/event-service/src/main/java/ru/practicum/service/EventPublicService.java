package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.entity.Event;
import ru.practicum.entity.EventPublicFilter;

import java.util.List;

public interface EventPublicService {

    List<EventShortDto> getAll(EventPublicFilter publicFilter, Integer from, Integer size,
                               HttpServletRequest httpServletRequest);

    EventFullDto getFullDtoById(Long eventId, Long userId, HttpServletRequest httpServletRequest);

    List<EventShortDto> getEventsShortByIds(List<Long> ids);

    Event getById(Long eventId);

    EventFullDto getEventFullInternal(Long eventId);

    EventShortDto getEventShort(Long eventId);

    Long countEventsByCategoryId(Long categoryId);

    void updateConfirmedRequests(Long eventId, Long confirmedRequests);

    List<EventFullDto> getRecommendations(Long userId);

    void likeEvent(Long eventId, Long userId);
}
