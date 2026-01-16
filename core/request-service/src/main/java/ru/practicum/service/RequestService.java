package ru.practicum.service;


import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto findById(Long requestId);

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getRequestsByEventId(Long eventId);

    EventRequestStatusUpdateResult updateRequestStatuses(
            EventRequestStatusUpdateRequest requestDto,
            Long eventId
    );

    Long getConfirmedRequestsCount(Long eventId);

    boolean isUserTakePart(Long userId, Long eventId);
}
