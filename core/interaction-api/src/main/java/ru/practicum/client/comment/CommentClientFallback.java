package ru.practicum.client.comment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CommentClientFallback implements CommentClient {

    @Override
    public Long getCountPublishedCommentsByEventId(Long eventId) {
        log.warn("Comment-service недоступен. Возвращаем 0 комментариев для события {}", eventId);
        return 0L;
    }
}
