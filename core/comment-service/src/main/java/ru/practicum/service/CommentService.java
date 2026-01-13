package ru.practicum.service;

import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentRequest;

import java.util.List;

public interface CommentService {
    CommentDto getCommentById(Long commentId);

    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    CommentDto updateCommentByUser(Long userId, Long commentId, UpdateCommentRequest request);

    void deleteCommentByUser(Long userId, Long commentId);

    List<CommentDto> getUserComments(Long userId, Integer from, Integer size);

    List<CommentDto> getEventComments(Long eventId, Integer from, Integer size);

    CommentDto moderateComment(Long commentId, Boolean approve);

    List<CommentDto> getCommentsForModeration(Integer from, Integer size);
}