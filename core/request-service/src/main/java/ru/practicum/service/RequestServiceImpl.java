package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.event.EventClient;
import ru.practicum.client.user.UserClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.entity.ParticipationRequest;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final RequestMapper requestMapper;

    @Override
    public ParticipationRequestDto findById(Long requestId) {
        return requestMapper.toDto(getById(requestId));
    }

    private ParticipationRequest getById(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id = %d не найден".formatted(requestId)));
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание заявки: пользователь {} хочет присоединиться к событию {}", userId, eventId);

        userClient.getUserById(userId);
        EventFullDto event = eventClient.getEventFullById(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            log.warn("Попытка инициатора {} создать заявку на своё событие {}", userId, eventId);
            throw new ConflictException("Инициатор события не может подать заявку на участие");
        }

        if (!"PUBLISHED".equals(event.getState())) {
            log.warn("Попытка создать заявку на неопубликованное событие {}: статус {}", eventId, event.getState());
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            log.warn("Заявка от пользователя {} на событие {} уже существует", userId, eventId);
            throw new ConflictException("Заявка от пользователя %s к событию %s уже существует".formatted(eventId, userId));
        }


        if (event.getParticipantLimit() > 0) {
            if (event.getConfirmedRequests() >= event.getParticipantLimit()) {
                log.warn("Достигнут лимит участников для события {}: confirmed={}, limit={}",
                        eventId, event.getConfirmedRequests(), event.getParticipantLimit());
                throw new ConflictException("Достигнут лимит участников");
            }
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
                .status(RequestStatus.PENDING)
                .build();

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);

            Long newConfirmed = event.getConfirmedRequests() + 1;
            eventClient.updateConfirmedRequests(eventId, newConfirmed); // Отправляем финальное значение
        }

        ParticipationRequest saved = requestRepository.save(request);
        log.info("Заявка успешно создана: ID={}, статус={}", saved.getId(), saved.getStatus());

        return requestMapper.toDto(saved, event);
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.debug("Получение всех заявок пользователя {}", userId);

        userClient.getUserById(userId);
        List<ParticipationRequest> requests = requestRepository.findByRequesterId(userId);

        Set<Long> eventIds = requests.stream()
                .map(ParticipationRequest::getEventId)
                .collect(Collectors.toSet());
        log.debug("Найдено {} заявок, связанных с событиями: {}", requests.size(), eventIds);

        Map<Long, EventFullDto> eventMap;

        if (!eventIds.isEmpty()) {
            log.debug("У пользователя {} нет заявок", userId);
            List<EventFullDto> events = eventClient.getEventsFullByIds(new ArrayList<>(eventIds));
            eventMap = events.stream()
                    .collect(Collectors.toMap(EventFullDto::getId, Function.identity()));
        } else {
            eventMap = new HashMap<>();
        }

        return requests.stream()
                .map(request -> {
                    EventFullDto event = eventMap.get(request.getEventId());
                    return requestMapper.toDto(request, event);
                })
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена заявки: пользователь {} отменяет заявку {}", userId, requestId);
        ParticipationRequest request = getById(requestId);

        if (!request.getRequesterId().equals(userId)) {
            log.warn("Пользователь {} пытается отменить чужую заявку {}", userId, requestId);
            throw new NotFoundException("Запрос с id = %d не найден для пользователя с userId = %d"
                    .formatted(userId, requestId));
        }

        request.setStatus(RequestStatus.CANCELED);
        ParticipationRequest updatedRequest = requestRepository.save(request);
        log.info("Заявка {} успешно отменена", requestId);

        return requestMapper.toDto(updatedRequest);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEventId(Long eventId) {
        return requestRepository.findByEventId(eventId)
                .stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatuses(EventRequestStatusUpdateRequest requestDto, Long eventId) {
        log.info("Обновление статусов заявок для события {}: {} заявок, новый статус = {}",
                eventId, requestDto.getRequestIds().size(), requestDto.getStatus());
        EventFullDto event = eventClient.getEventFullById(eventId);

        if (!"PUBLISHED".equals(event.getState())) {
            log.warn("Попытка обновить заявки для неопубликованного события {}: статус {}", eventId, event.getState());
            throw new ConflictException("Событие не опубликовано");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(requestDto.getRequestIds());

        for (ParticipationRequest request : requests) {
            if (!RequestStatus.PENDING.equals(request.getStatus())) {
                throw new ConflictException("Заявка не в состоянии ожидания");
            }
        }

        if (RequestStatus.CONFIRMED.equals(requestDto.getStatus())) {
            Long currentConfirmed = event.getConfirmedRequests();
            Long totalAfterConfirmation = currentConfirmed + requests.size(); // Сколько будет после подтверждения

            log.info("Проверка лимита. eventId={}, participantLimit={}, currentConfirmed={}, totalAfterConfirmation={}",
                    eventId, event.getParticipantLimit(), currentConfirmed, totalAfterConfirmation);

            if (totalAfterConfirmation > event.getParticipantLimit()) {
                log.warn("Превышен лимит участников для события {}: лимит={}, попытка подтвердить {} заявок",
                        eventId, event.getParticipantLimit(), requests.size());
                throw new ConflictException("Достигнут лимит участников");
            }
        }

        List<ParticipationRequest> confirmedRequests = new ArrayList<>();
        List<ParticipationRequest> rejectedRequests = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            if (RequestStatus.CONFIRMED.equals(requestDto.getStatus())) {
                request.setStatus(RequestStatus.CONFIRMED);
                confirmedRequests.add(request);
            } else {
                request.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(request);
            }
        }

        requestRepository.saveAll(requests);
        log.info("Сохранено {} заявок: {} подтверждено, {} отклонено",
                requests.size(), confirmedRequests.size(), rejectedRequests.size());

        Long finalConfirmedCount = requestRepository.countConfirmedRequestsByEventId(eventId);
        eventClient.updateConfirmedRequests(eventId, finalConfirmedCount);

        List<ParticipationRequestDto> confirmedDtos = confirmedRequests.stream()
                .map(req -> requestMapper.toDto(req, event))
                .toList();

        List<ParticipationRequestDto> rejectedDtos = rejectedRequests.stream()
                .map(req -> requestMapper.toDto(req, event))
                .toList();

        return new EventRequestStatusUpdateResult(confirmedDtos, rejectedDtos);
    }

    @Override
    public Long getConfirmedRequestsCount(Long eventId) {
        return requestRepository.countConfirmedRequestsByEventId(eventId);
    }
}
