package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.entity.Event;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findAll(Specification<Event> spec, Pageable pageable);
    Page<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    Long countByCategoryId(Long categoryId);
}


