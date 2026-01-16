package ru.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.AnalyzerGrpcClient;
import ru.practicum.client.category.CategoryClient;
import ru.practicum.client.request.RequestClient;
import ru.practicum.client.user.UserClient;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.Event;
import ru.practicum.entity.Location;
import ru.practicum.entity.UpdateEventUserRequest;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.LocationRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventPrivateServiceImpl implements EventPrivateService {

    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;
    private final EventPublicService eventPublicService;
    private final CategoryClient categoryClient;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final AnalyzerGrpcClient analyzerGrpcClient;

    @Override
    public List<EventShortDto> getAll(Long userId, Integer from, Integer size) {
        userClient.getUserById(userId);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable).getContent();

        // Подверждённые заявки
        Map<Long, Long> confirmedRequestsMap = events.stream()
                .collect(Collectors.toMap(
                        Event::getId,
                        event -> requestClient.getConfirmedRequestsCount(event.getId())
                ));

        // Категории
        Set<Long> categoryIds = events.stream().map(Event::getCategoryId).collect(Collectors.toSet());
        Map<Long, CategoryDto> categoryMap = categoryIds.isEmpty() ? new HashMap<>() :
                categoryClient.getCategoriesByIds(new ArrayList<>(categoryIds))
                        .stream()
                        .collect(Collectors.toMap(CategoryDto::getId, Function.identity()));

        // Рейтинги
        Map<Long, Double> ratings = fetchEventRatings(events);

        return events.stream()
                .map(event -> {
                    UserShortDto author = UserShortDto.builder()
                            .id(event.getInitiatorId())
                            .name("[Current User]")
                            .build();
                    CategoryDto category = categoryMap.get(event.getCategoryId());
                    EventShortDto dto = eventMapper.toEventShortDtoWithConfirmed(
                            event, confirmedRequestsMap.get(event.getId()), author, category);
                    dto.setRating(ratings.getOrDefault(event.getId(), 0.0));
                    return dto;
                })
                .toList();
    }

    private Map<Long, Double> fetchEventRatings(List<Event> events) {
        if (events.isEmpty()) {
            return new HashMap<>();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();

        return analyzerGrpcClient.getInteractionsCount(request)
                .stream()
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore
                ));
    }

    @Override
    @Transactional
    public EventFullDto create(NewEventDto newEventDto, Long userId) {
        if (newEventDto.getEventDate().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Указана дата начала события в прошлом");
        }

        categoryClient.getCategoryById(newEventDto.getCategory());
        userClient.getUserById(userId);

        Location location = locationRepository.save(locationMapper.toLocation(newEventDto.getLocation()));

        Event event = Event.builder()
                .annotation(newEventDto.getAnnotation())
                .categoryId(newEventDto.getCategory())
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .initiatorId(userId)
                .location(location)
                .paid(newEventDto.getPaid())
                .participantLimit(newEventDto.getParticipantLimit())
                .requestModeration(newEventDto.getRequestModeration())
                .title(newEventDto.getTitle())
                .createdOn(LocalDateTime.now())
                .confirmedRequests(0L)
                .state(EventState.PENDING)
                .build();

        Event saved = eventRepository.save(event);

        UserShortDto author = userClient.getUserById(saved.getInitiatorId());
        CategoryDto category = categoryClient.getCategoryById(saved.getCategoryId());
        EventFullDto dto = eventMapper.toEventFullDto(saved, author, category, locationMapper.toLocationDto(saved.getLocation()));

        dto.setRating(0.0);

        return dto;
    }

    @Override
    public EventFullDto getByInitiatorId(Long userId, Long eventId) {
        userClient.getUserById(userId);
        Event event = eventPublicService.getById(eventId);

        UserShortDto author = userClient.getUserById(event.getInitiatorId());
        CategoryDto category = categoryClient.getCategoryById(event.getCategoryId());
        Long confirmed = requestClient.getConfirmedRequestsCount(eventId);
        event.setConfirmedRequests(confirmed);

        EventFullDto dto = eventMapper.toEventFullDto(event, author, category, locationMapper.toLocationDto(event.getLocation()));

        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addEventId(eventId)
                .build();
        List<RecommendedEventProto> responses = analyzerGrpcClient.getInteractionsCount(request);
        double rating = responses.isEmpty() ? 0.0 : responses.getFirst().getScore();
        dto.setRating(rating);

        return dto;
    }

    @Override
    public EventFullDto update(UpdateEventUserRequest request, Long userId, Long eventId) {
        Event event = checkUpdateEvent(userId, eventId);

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Событие уже опубликовано");
        }

        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Время события указано раньше, чем через два часа");
        }

        if (request.getAnnotation() != null && !request.getAnnotation().isBlank()) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            CategoryDto category = categoryClient.getCategoryById(request.getCategory().getId());
            event.setCategoryId(category.getId());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            event.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            event.setLocation(locationRepository.save(locationMapper.toLocation(request.getLocation())));
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            if (request.getParticipantLimit() < 0) {
                throw new ValidationException("Нельзя установить отрицательное значение лимита");
            }
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            event.setTitle(request.getTitle());
        }
        if (request.getEventDate() != null) {
            setEventDate(event, String.valueOf(request.getEventDate()));
        }
        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
                case REJECT_EVENT -> event.setState(EventState.REJECT);
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case PUBLISH_EVENT -> event.setState(EventState.PUBLISHED);
            }
        }

        Event updated = eventRepository.save(event);
        UserShortDto author = userClient.getUserById(updated.getInitiatorId());
        CategoryDto category = categoryClient.getCategoryById(updated.getCategoryId());
        EventFullDto dto = eventMapper.toEventFullDto(updated, author, category, locationMapper.toLocationDto(updated.getLocation()));

        // Актуальный рейтинг
        InteractionsCountRequestProto interactionsRequest = InteractionsCountRequestProto.newBuilder()
                .addEventId(eventId)
                .build();
        List<RecommendedEventProto> ratingResponse = analyzerGrpcClient.getInteractionsCount(interactionsRequest);
        double rating = ratingResponse.isEmpty() ? 0.0 : ratingResponse.getFirst().getScore();
        dto.setRating(rating);

        return dto;
    }

    @Override
    public List<ParticipationRequestDto> getRequests(Long userId, Long eventId) {
        userClient.getUserById(userId);

        Event event = eventPublicService.getById(eventId);
        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new ValidationException("Пользователь не является создателем события");
        }

        return requestClient.getRequestsByEventId(eventId);
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequest(EventRequestStatusUpdateRequest requestDto, Long userId,
                                                        Long eventId) {
        userClient.getUserById(userId);
        Event event = eventPublicService.getById(eventId);

        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new ValidationException("Пользователь не является создателем события");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Событие не опубликовано");
        }

        try {
            EventRequestStatusUpdateResult eventRequestStatusUpdateResult = requestClient.updateRequestStatuses(requestDto, eventId);
            log.info("Получен ответ от request-service: {}", eventRequestStatusUpdateResult);
            return eventRequestStatusUpdateResult;
        } catch (FeignException.Conflict e) {
            throw new ConflictException("Достигнут лимит участников");
        }
    }

    private Event checkUpdateEvent(Long userId, Long eventId) {
        Event event = eventPublicService.getById(eventId);
        userClient.getUserById(userId);

        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new ValidationException("Пользователь не является создателем события");
        }

        return event;
    }

    private void setEventDate(Event event, String date) {
        if (date != null) {
            String normalizedDate = date.replace('T', ' ');
            LocalDateTime eventDateTime = LocalDateTime.parse(normalizedDate,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            if (eventDateTime.isBefore(LocalDateTime.now())) {
                throw new ValidationException("Указанная дата уже наступила");
            }
            event.setEventDate(eventDateTime);
        }
    }

}
