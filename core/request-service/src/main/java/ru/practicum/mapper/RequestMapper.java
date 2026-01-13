package ru.practicum.mapper;

import org.mapstruct.Mapper;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.entity.ParticipationRequest;

@Mapper(componentModel = "spring")
public interface RequestMapper {
    default ParticipationRequestDto toDto(ParticipationRequest request, EventFullDto event) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(event != null ? event.getId() : request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus().toString())
                .build();
    }

    default ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus().toString())
                .build();
    }

}
