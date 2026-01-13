package ru.practicum.client.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

@Validated
@FeignClient(
        name = "request-service",
        path = "/internal/requests",
        fallback = RequestClientFallback.class
)
public interface RequestClient {
    @GetMapping("/event/{eventId}")
    List<ParticipationRequestDto> getRequestsByEventId(@PathVariable Long eventId);

    @PostMapping("/event/{eventId}/update")
    EventRequestStatusUpdateResult updateRequestStatuses(
            @RequestBody EventRequestStatusUpdateRequest request,
            @PathVariable Long eventId
    );

    @GetMapping("/event/{eventId}/confirmed-count")
    Long getConfirmedRequestsCount(@PathVariable Long eventId);

}
