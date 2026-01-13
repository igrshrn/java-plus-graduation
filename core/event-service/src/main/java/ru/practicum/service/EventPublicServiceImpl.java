package ru.practicum.service;

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
import ru.practicum.client.StatClient;
import ru.practicum.client.category.CategoryClient;
import ru.practicum.client.comment.CommentClient;
import ru.practicum.client.user.UserClient;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.Event;
import ru.practicum.entity.EventPublicFilter;
import ru.practicum.entity.EventSort;
import ru.practicum.ewm.dto.EndpointHit;
import ru.practicum.ewm.dto.StatRequest;
import ru.practicum.ewm.dto.ViewStatDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.repository.EventRepository;

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
    private final StatClient statClient;
    private final EventMapper eventMapper;
    private final CommentClient commentClient;
    private final UserClient userClient;
    private final CategoryClient categoryClient;
    private final LocationMapper locationMapper;

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
        hit(httpServletRequest);

        Page<Event> page = eventRepository.findAll(specification, PageRequest.of(from / size, size).withSort(sort));
        List<Event> events = page.getContent();

        return getEventShortDtos(events);
    }

    @NotNull
    private List<EventShortDto> getEventShortDtos(List<Event> events) {
        Set<Long> authorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());
        Set<Long> categoryIds = events.stream()
                .map(Event::getCategoryId)
                .collect(Collectors.toSet());

        Map<Long, UserShortDto> authorMap;
        if (!authorIds.isEmpty()) {
            List<UserShortDto> authors = userClient.getUsersByIds(new ArrayList<>(authorIds));
            authorMap = authors.stream()
                    .collect(Collectors.toMap(UserShortDto::getId, Function.identity()));
        } else {
            authorMap = new HashMap<>();
        }

        Map<Long, CategoryDto> categoryMap;
        if (!categoryIds.isEmpty()) {
            List<CategoryDto> categories = categoryClient.getCategoriesByIds(new ArrayList<>(categoryIds));
            categoryMap = categories.stream()
                    .collect(Collectors.toMap(CategoryDto::getId, Function.identity()));
        } else {
            categoryMap = new HashMap<>();
        }

        return events.stream()
                .map(event -> {
                    UserShortDto author = authorMap.get(event.getInitiatorId());
                    CategoryDto category = categoryMap.get(event.getCategoryId());
                    return eventMapper.toEventShortDto(event, author, category);
                })
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto getFullDtoById(Long eventId, HttpServletRequest httpServletRequest) {
        Event event = getById(eventId);
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("getById: Событие id = %d не опубликовано".formatted(eventId));
        }
        hit(httpServletRequest);
        StatRequest statsRequest = StatRequest.builder()
                .start(event.getPublishedOn())
                .end(LocalDateTime.now())
                .uris(List.of("/events/" + eventId))
                .unique(true)
                .build();
        List<ViewStatDto> stats = statClient.getStats(statsRequest);
        log.info("Метод getById, длина списка stats: {}", stats.size());
        Long views = stats.isEmpty() ? 0L : stats.getFirst().getHits();
        event.setViews(views);
        eventRepository.save(event);
        log.info("Метод getById, количество сохраняемых просмотров: {}", views);

        EventFullDto eventFullDto = getEventFullInternal(eventId);
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

        return eventMapper.toEventShortDto(event, author, category);
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

    private void hit(HttpServletRequest httpServletRequest) {
        EndpointHit hitDtoRequest = new EndpointHit(
                null,
                "main-server",
                httpServletRequest.getRequestURI(),
                httpServletRequest.getRemoteAddr(),
                LocalDateTime.now()
        );
        statClient.saveHit(hitDtoRequest);
    }

    @Override
    @Transactional
    public void updateConfirmedRequests(Long eventId, Long confirmedRequests) {
        Event event = getById(eventId);
        log.info("UpdateConfirmed. EventId = {}, Confirmed = {}", eventId, confirmedRequests);
        event.setConfirmedRequests(confirmedRequests);
        eventRepository.save(event);
    }
}