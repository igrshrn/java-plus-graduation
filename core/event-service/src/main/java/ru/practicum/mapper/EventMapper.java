package ru.practicum.mapper;

import org.mapstruct.Mapper;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.LocationDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.Event;

@Mapper(componentModel = "spring", uses = {LocationMapper.class})
public interface EventMapper {
    default EventFullDto toEventFullDto(Event event, UserShortDto initiator, CategoryDto category, LocationDto location) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(category)
                .confirmedRequests(event.getConfirmedRequests())
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(initiator)
                .location(location)
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState() != null ? event.getState().name() : null)
                .title(event.getTitle())
                .commentsCount(0L)
                .build();
    }

    default EventShortDto toEventShortDto(Event event, UserShortDto initiator, CategoryDto category) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(category)
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(event.getEventDate())
                .initiator(initiator)
                .paid(event.getPaid())
                .title(event.getTitle())
                .commentsCount(0L)
                .build();
    }

    default EventShortDto toEventShortDtoWithConfirmed(
            Event event,
            Long confirmedRequests,
            UserShortDto initiator,
            CategoryDto category) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(category)
                .confirmedRequests(confirmedRequests)
                .eventDate(event.getEventDate())
                .initiator(initiator)
                .paid(event.getPaid())
                .title(event.getTitle())
                .commentsCount(0L)
                .build();
    }

}