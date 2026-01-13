package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.event.LocationDto;
import ru.practicum.entity.Location;

@Mapper(componentModel = "spring")
public interface LocationMapper {
    LocationDto toLocationDto(Location location);

    @Mapping(target = "id", ignore = true)
    Location toLocation(LocationDto locationDto);
}
