package ru.practicum.client.event;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;

import java.util.List;

@FeignClient(
        name = "event-service",
        path = "/internal/events",
        fallback = EventClientFallback.class
)
public interface EventClient {
    @GetMapping("/{id}")
    EventShortDto getEventById(@PathVariable Long id);

    @GetMapping("/{id}/full")
    EventFullDto getEventFullById(@PathVariable Long id);

    @PostMapping("/batch")
    List<EventShortDto> getEventsByIds(@RequestBody List<Long> ids);

    @PostMapping("/batch/full")
    List<EventFullDto> getEventsFullByIds(@RequestBody List<Long> ids);

    @GetMapping("/count-by-category/{categoryId}")
    Long countEventsByCategoryId(@PathVariable Long categoryId);

    @PostMapping("/{eventId}/confirmed-requests")
    void updateConfirmedRequests(@PathVariable Long eventId, @RequestBody Long confirmedRequests);
}
