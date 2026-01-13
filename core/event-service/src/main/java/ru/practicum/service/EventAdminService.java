package ru.practicum.service;


import ru.practicum.dto.event.EventFullDto;
import ru.practicum.entity.EventAdminFilter;
import ru.practicum.entity.UpdateEventAdminRequest;

import java.util.List;

public interface EventAdminService {

    List<EventFullDto> getAll(EventAdminFilter adminFilter, Integer from, Integer size);

    EventFullDto update(UpdateEventAdminRequest request, Long eventId);
}
