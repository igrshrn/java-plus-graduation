package ru.practicum.client.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

@Slf4j
@Component
public class RequestClientFallback implements RequestClient {

    @Override
    public List<ParticipationRequestDto> getRequestsByEventId(Long eventId) {
        log.warn("Request-service недоступен. Возвращаем пустой список заявок для события {}", eventId);
        return List.of();
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatuses(
            EventRequestStatusUpdateRequest request,
            Long eventId) {
        log.warn("Request-service недоступен. Невозможно обновить статусы заявок для события {}", eventId);
        return new EventRequestStatusUpdateResult(List.of(), List.of());
    }

    @Override
    public Long getConfirmedRequestsCount(Long eventId) {
        log.warn("Request-service недоступен. Возвращаем 0 для confirmedRequests события {}", eventId);
        return 0L;
    }

    @Override
    public boolean isUserTakePart(Long userId, Long eventId) {
        log.warn("Request-service недоступен. Возвращаем false для isUserTakePart пользователем {}, события {}", eventId, eventId);
        return false;
    }
}
