package ru.practicum.mapper;

import org.mapstruct.Mapper;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    default CommentDto toCommentDto(Comment comment, UserShortDto author) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(author)
                .eventId(comment.getEventId())
                .created(comment.getCreated())
                .updated(comment.getUpdated())
                .status(comment.getStatus())
                .build();
    }
}