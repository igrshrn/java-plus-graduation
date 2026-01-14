package ru.practicum.client.comment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "comment-service",
        path = "/internal/comments",
        fallback = CommentClientFallback.class
)
public interface CommentClient {
    @GetMapping("/count-published-comments/{eventId}")
    Long getCountPublishedCommentsByEventId(@PathVariable Long eventId);
}
