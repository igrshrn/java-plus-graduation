package ru.practicum.service;


import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.entity.UpdateEventUserRequest;

import java.util.List;

public interface EventPrivateService {

    List<EventShortDto> getAll(Long userId, Integer from, Integer size);

    EventFullDto create(NewEventDto newEventDto, Long userId);

    EventFullDto getByInitiatorId(Long userId, Long eventId);

    EventFullDto update(UpdateEventUserRequest request, Long userId, Long eventId);

    List<ParticipationRequestDto> getRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequest(EventRequestStatusUpdateRequest request, Long userId,
                                                 Long eventId);
}
