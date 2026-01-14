package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.repository.CommentRepository;

@Service
@RequiredArgsConstructor
public class CommentCountService {
    private final CommentRepository commentRepository;

    public Long getCountPublishedCommentsByEventId(Long eventId) {
        return commentRepository.countPublishedCommentsByEventId(eventId);
    }
}
