package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.event.EventClient;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.entity.Compilation;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.repository.CompilationRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final CompilationMapper compilationMapper;
    private final EventClient eventClient;

    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto newCompilationDto) {
        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            eventClient.getEventsByIds(newCompilationDto.getEvents());
        }

        Compilation compilation = compilationMapper.toCompilation(newCompilationDto);
        Compilation saved = compilationRepository.save(compilation);

        List<EventShortDto> events = (newCompilationDto.getEvents() != null)
                ? eventClient.getEventsByIds(newCompilationDto.getEvents())
                : List.of();

        return compilationMapper.toCompilationDto(saved, events);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationRequest request) {
        Compilation compilation = findById(compId);

        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getEvents() != null) {
            eventClient.getEventsByIds(request.getEvents());
            compilation.setEventIds(request.getEvents());
        }

        Compilation updated = compilationRepository.save(compilation);

        List<EventShortDto> events = (request.getEvents() != null)
                ? eventClient.getEventsByIds(request.getEvents())
                : eventClient.getEventsByIds(updated.getEventIds());

        return compilationMapper.toCompilationDto(updated, events);
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        compilationRepository.deleteById(compId);
        log.info("Удалена подборка событий с id " + compId);
    }

    @Override
    public List<CompilationDto> get(Boolean pinned, int from, int size) {
        PageRequest pageRequest = PageRequest.of(from, size);
        List<Compilation> compilations = (pinned == null)
                ? compilationRepository.findAllBy(pageRequest)
                : compilationRepository.findByPinned(pinned, pageRequest);

        Set<Long> allEventIds = compilations.stream()
                .flatMap(c -> c.getEventIds().stream())
                .collect(Collectors.toSet());

        Map<Long, EventShortDto> eventMap;
        if (!allEventIds.isEmpty()) {
            List<EventShortDto> events = eventClient.getEventsByIds(new ArrayList<>(allEventIds));
            eventMap = events.stream()
                    .collect(Collectors.toMap(EventShortDto::getId, Function.identity()));
        } else {
            eventMap = new HashMap<>();
        }

        return compilations.stream()
                .map(compilation -> {
                    List<EventShortDto> events = compilation.getEventIds().stream()
                            .map(eventMap::get)
                            .filter(Objects::nonNull)
                            .toList();
                    return compilationMapper.toCompilationDto(compilation, events);
                })
                .toList();
    }

    @Override
    public CompilationDto getById(Long compId) {
        Compilation compilation = findById(compId);
        List<EventShortDto> events = eventClient.getEventsByIds(compilation.getEventIds());
        return compilationMapper.toCompilationDto(compilation, events);
    }

    private Compilation findById(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка событий с id " + compId + " не найдена"));
    }
}
