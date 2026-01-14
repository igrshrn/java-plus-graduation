package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.client.comment.CommentClient;
import ru.practicum.service.CommentCountService;

@RestController
@RequestMapping("/internal/comments")
@RequiredArgsConstructor
public class InternalCommentController implements CommentClient {
    private final CommentCountService commentCountService;

    public Long getCountPublishedCommentsByEventId(@PathVariable Long eventId) {
        return commentCountService.getCountPublishedCommentsByEventId(eventId);
    }
}
