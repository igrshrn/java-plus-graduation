package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.client.request.RequestClient;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController implements RequestClient {
    private final RequestService requestService;

    @Override
    public List<ParticipationRequestDto> getRequestsByEventId(Long eventId) {
        return requestService.getRequestsByEventId(eventId);
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatuses(
            EventRequestStatusUpdateRequest requestDto,
            Long eventId) {
        return requestService.updateRequestStatuses(requestDto, eventId);
    }

    @Override
    public Long getConfirmedRequestsCount(Long eventId) {
        return requestService.getConfirmedRequestsCount(eventId);
    }
}
