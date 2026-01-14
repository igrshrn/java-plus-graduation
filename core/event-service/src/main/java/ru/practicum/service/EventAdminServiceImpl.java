package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.category.CategoryClient;
import ru.practicum.client.user.UserClient;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.event.LocationDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.Event;
import ru.practicum.entity.EventAdminFilter;
import ru.practicum.entity.UpdateEventAdminRequest;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.LocationRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventAdminServiceImpl implements EventAdminService {

    private final EventRepository eventRepository;
    private final CategoryClient categoryClient;
    private final UserClient userClient;
    private final LocationRepository locationRepository;
    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;
    private final EventPublicService eventPublicService;

    @Override
    public List<EventFullDto> getAll(EventAdminFilter adminFilter, Integer from, Integer size) {
        Specification<Event> specification = DbSpecification.getAdminSpecification(
                adminFilter.getUserIds(),
                adminFilter.getStates(),
                adminFilter.getCategoryIds(),
                adminFilter.getRangeStart(),
                adminFilter.getRangeEnd());
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAll(specification, pageable).getContent();

        Set<Long> authorIds = events.stream().map(Event::getInitiatorId).collect(Collectors.toSet());
        Set<Long> categoryIds = events.stream().map(Event::getCategoryId).collect(Collectors.toSet());
        Map<Long, UserShortDto> authorMap = fetchAuthors(authorIds);
        Map<Long, CategoryDto> categoryMap = fetchCategories(categoryIds);

        return events.stream()
                .map(event -> {
                    UserShortDto author = authorMap.get(event.getInitiatorId());
                    CategoryDto category = categoryMap.get(event.getCategoryId());
                    LocationDto location = locationMapper.toLocationDto(event.getLocation());
                    return eventMapper.toEventFullDto(event, author, category, location);
                })
                .sorted(Comparator.comparingLong(EventFullDto::getId).reversed())
                .toList();
    }

    private Map<Long, UserShortDto> fetchAuthors(Set<Long> authorIds) {
        if (authorIds.isEmpty()) return new HashMap<>();
        List<UserShortDto> authors = userClient.getUsersByIds(new ArrayList<>(authorIds));
        return authors.stream().collect(Collectors.toMap(UserShortDto::getId, Function.identity()));
    }

    private Map<Long, CategoryDto> fetchCategories(Set<Long> categoryIds) {
        if (categoryIds.isEmpty()) return new HashMap<>();
        List<CategoryDto> categories = categoryClient.getCategoriesByIds(new ArrayList<>(categoryIds));
        return categories.stream().collect(Collectors.toMap(CategoryDto::getId, Function.identity()));
    }

    @Override
    @Transactional
    public EventFullDto update(UpdateEventAdminRequest request, Long eventId) {
        Event event = eventPublicService.getById(eventId);

        if (request.getAnnotation() != null && !request.getAnnotation().isBlank()) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            CategoryDto categoryById = categoryClient.getCategoryById(request.getCategory());
            event.setCategoryId(categoryById.getId());
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
                case PUBLISH_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Событие уже опубликовано");
                    }
                    if (event.getState() == EventState.REJECT) {
                        throw new ConflictException("Событие отменено");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;

                case REJECT_EVENT:
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Нельзя отменить опубликованное событие");
                    }
                    event.setState(EventState.REJECT);
                    break;
            }
        }

        Event updatedEvent = eventRepository.save(event);
        UserShortDto author = userClient.getUserById(updatedEvent.getInitiatorId());
        CategoryDto category = categoryClient.getCategoryById(updatedEvent.getCategoryId());
        LocationDto location = locationMapper.toLocationDto(updatedEvent.getLocation());
        return eventMapper.toEventFullDto(updatedEvent, author, category, location);
    }

    private void setEventDate(Event event, String date) {
        if (date != null) {
            LocalDateTime eventDateTime;

            try {
                eventDateTime = LocalDateTime.parse(date, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) {
                try {
                    eventDateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (DateTimeParseException ex) {
                    throw new ValidationException("Неверный формат даты. Используйте yyyy-MM-ddTHH:mm:ss или yyyy-MM-dd HH:mm:ss");
                }
            }

            if (eventDateTime.isBefore(LocalDateTime.now())) {
                throw new ValidationException("Указанная дата уже наступила");
            }
            event.setEventDate(eventDateTime);
        }
    }
}