package ru.practicum.service;

import com.google.protobuf.Timestamp;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.AnalyzerGrpcClient;
import ru.practicum.client.CollectorGrpcClient;
import ru.practicum.client.category.CategoryClient;
import ru.practicum.client.comment.CommentClient;
import ru.practicum.client.request.RequestClient;
import ru.practicum.client.user.UserClient;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.Event;
import ru.practicum.entity.EventPublicFilter;
import ru.practicum.entity.EventSort;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.repository.EventRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventPublicServiceImpl implements EventPublicService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final CommentClient commentClient;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final CategoryClient categoryClient;
    private final LocationMapper locationMapper;
    private final AnalyzerGrpcClient analyzerGrpcClient;
    private final CollectorGrpcClient collectorGrpcClient;

    @Override
    public List<EventShortDto> getAll(EventPublicFilter publicFilter, Integer from, Integer size,
                                      HttpServletRequest httpServletRequest) {
        publicFilter.validateDates();
        Specification<Event> specification = DbSpecification.getPublicSpecification(
                publicFilter.getText(),
                publicFilter.getCategoryIds(),
                publicFilter.getPaid(),
                publicFilter.getRangeStart(),
                publicFilter.getRangeEnd(),
                publicFilter.getOnlyAvailable());
        Sort sort = Optional.ofNullable(publicFilter.getSort())
                .map(s -> Sort.by(Sort.Direction.DESC, s == EventSort.EVENT_DATE ? "eventDate" : "views"))
                .orElse(Sort.unsorted());

        Page<Event> page = eventRepository.findAll(specification, PageRequest.of(from / size, size).withSort(sort));
        List<Event> events = page.getContent();

        return getEventShortDtos(events);
    }

    @NotNull
    private List<EventShortDto> getEventShortDtos(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> authorIds = events.stream().map(Event::getInitiatorId).collect(Collectors.toSet());
        Set<Long> categoryIds = events.stream().map(Event::getCategoryId).collect(Collectors.toSet());
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();

        Map<Long, Double> ratingMap = analyzerGrpcClient.getInteractionsCount(request)
                .stream()
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore
                ));

        Map<Long, UserShortDto> authorMap = authorIds.isEmpty() ? new HashMap<>() :
                userClient.getUsersByIds(new ArrayList<>(authorIds))
                        .stream()
                        .collect(Collectors.toMap(UserShortDto::getId, Function.identity()));

        Map<Long, CategoryDto> categoryMap = categoryIds.isEmpty() ? new HashMap<>() :
                categoryClient.getCategoriesByIds(new ArrayList<>(categoryIds))
                        .stream()
                        .collect(Collectors.toMap(CategoryDto::getId, Function.identity()));

        return events.stream()
                .map(event -> {
                    EventShortDto dto = eventMapper.toEventShortDto(
                            event,
                            authorMap.get(event.getInitiatorId()),
                            categoryMap.get(event.getCategoryId())
                    );
                    dto.setRating(ratingMap.getOrDefault(event.getId(), 0.0));

                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto getFullDtoById(Long eventId, Long userId, HttpServletRequest httpServletRequest) {
        Event event = getById(eventId);

        collectorGrpcClient.sendUserAction(
                createUserAction(eventId, userId, ActionTypeProto.ACTION_VIEW, Instant.now()));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие id = %d не опубликовано".formatted(eventId));
        }

        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addEventId(eventId)
                .build();
        List<RecommendedEventProto> responses = analyzerGrpcClient.getInteractionsCount(request);
        double rating = responses.isEmpty() ? 0.0 : responses.getFirst().getScore();

        EventFullDto eventFullDto = getEventFullInternal(eventId);
        eventFullDto.setRating(rating);

        Long commentsCount = commentClient.getCountPublishedCommentsByEventId(eventId);
        eventFullDto.setCommentsCount(commentsCount);

        return eventFullDto;
    }

    @Override
    public List<EventShortDto> getEventsShortByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Event> events = eventRepository.findAllById(ids);

        return getEventShortDtos(events);
    }

    @Override
    public EventFullDto getEventFullInternal(Long eventId) {
        Event event = getById(eventId);

        UserShortDto author = userClient.getUserById(event.getInitiatorId());
        CategoryDto category = categoryClient.getCategoryById(event.getCategoryId());

        return eventMapper.toEventFullDto(event, author, category, locationMapper.toLocationDto(event.getLocation()));
    }

    @Override
    public EventShortDto getEventShort(Long eventId) {
        Event event = getById(eventId);
        UserShortDto author = userClient.getUserById(event.getInitiatorId());
        CategoryDto category = categoryClient.getCategoryById(event.getCategoryId());

        EventShortDto dto = eventMapper.toEventShortDto(event, author, category);

        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addEventId(eventId)
                .build();
        List<RecommendedEventProto> responses = analyzerGrpcClient.getInteractionsCount(request);
        double rating = responses.isEmpty() ? 0.0 : responses.getFirst().getScore();
        dto.setRating(rating);

        return dto;
    }

    @Override
    public Long countEventsByCategoryId(Long categoryId) {
        return eventRepository.countByCategoryId(categoryId);
    }

    @Override
    public Event getById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие id = %d не найдено".formatted(eventId)));
    }

    @Override
    @Transactional
    public void updateConfirmedRequests(Long eventId, Long confirmedRequests) {
        Event event = getById(eventId);
        log.info("UpdateConfirmed. EventId = {}, Confirmed = {}", eventId, confirmedRequests);
        event.setConfirmedRequests(confirmedRequests);
        eventRepository.save(event);
    }

    @Override
    public List<EventFullDto> getRecommendations(Long userId) {
        return List.of();
    }

    @Override
    public void likeEvent(Long eventId, Long userId) {

        if (!requestClient.isUserTakePart(userId, eventId)) {
            throw new ValidationException("Пользователь " + userId + " не принимал участие в событии " + eventId);
        }

        UserActionProto action = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(ActionTypeProto.ACTION_LIKE)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(Instant.now().getEpochSecond())
                        .setNanos(Instant.now().getNano())
                        .build())
                .build();

        collectorGrpcClient.sendUserAction(action);
    }

    private UserActionProto createUserAction(Long eventId, Long userId, ActionTypeProto type, Instant timestamp) {
        return UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(type)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(timestamp.getEpochSecond())
                        .setNanos(timestamp.getNano())
                        .build())
                .build();
    }
}