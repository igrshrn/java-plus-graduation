package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.client.event.EventClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.service.EventPublicService;

import java.util.List;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController implements EventClient {

    private final EventPublicService eventPublicService;

    @Override
    public EventShortDto getEventById(Long id) {
        return eventPublicService.getEventShort(id);
    }

    @Override
    public EventFullDto getEventFullById(Long id) {
        return eventPublicService.getEventFullInternal(id);
    }

    @Override
    public List<EventShortDto> getEventsByIds(List<Long> ids) {
        return eventPublicService.getEventsShortByIds(ids);
    }

    @Override
    public List<EventFullDto> getEventsFullByIds(List<Long> ids) {
        return List.of();
    }

    @Override
    public Long countEventsByCategoryId(Long categoryId) {
        return eventPublicService.countEventsByCategoryId(categoryId);
    }

    @Override
    public void updateConfirmedRequests(Long eventId, Long confirmedRequests) {
        eventPublicService.updateConfirmedRequests(eventId, confirmedRequests);
    }
}
