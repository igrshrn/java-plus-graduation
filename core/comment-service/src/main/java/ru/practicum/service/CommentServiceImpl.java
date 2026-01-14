package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.event.EventClient;
import ru.practicum.client.user.UserClient;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.CommentStatus;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentRequest;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.Comment;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.repository.CommentRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final CommentMapper commentMapper;

    private Comment findById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id = %d не найден".formatted(commentId)));
    }

    @Override
    public CommentDto getCommentById(Long commentId) {
        Comment comment = findById(commentId);

        UserShortDto author = userClient.getUserById(comment.getAuthorId());
        return commentMapper.toCommentDto(comment, author);
    }

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        userClient.getUserById(userId);
        EventFullDto event = eventClient.getEventFullById(eventId);

        if (!"PUBLISHED".equals(event.getState())) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        if (commentRepository.existsByEventIdAndAuthorId(eventId, userId)) {
            throw new ConflictException("Вы уже оставляли комментарий к этому событию");
        }

        Comment comment = Comment.builder()
                .text(newCommentDto.getText())
                .authorId(userId)
                .eventId(event.getId())
                .created(LocalDateTime.now())
                .status(CommentStatus.PENDING)
                .build();

        Comment saved = commentRepository.save(comment);
        UserShortDto author = userClient.getUserById(saved.getAuthorId());

        return commentMapper.toCommentDto(comment, author);
    }

    @Override
    @Transactional
    public CommentDto updateCommentByUser(Long userId, Long commentId, UpdateCommentRequest request) {
        userClient.getUserById(userId);
        Comment comment = getCommentByIdAndAuthor(commentId, userId);

        if (comment.getStatus() == CommentStatus.DELETED) {
            throw new ConflictException("Нельзя редактировать удаленный комментарий");
        }

        comment.setText(request.getText());
        comment.setUpdated(LocalDateTime.now());
        comment.setStatus(CommentStatus.PENDING);
        Comment updated = commentRepository.save(comment);

        UserShortDto author = userClient.getUserById(updated.getAuthorId());
        return commentMapper.toCommentDto(comment, author);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        userClient.getUserById(userId);
        Comment comment = getCommentByIdAndAuthor(commentId, userId);
        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, Integer from, Integer size) {
        userClient.getUserById(userId);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findByAuthorId(userId, pageable);

        Map<Long, UserShortDto> authorMap = fetchAuthorsMap(comments);

        return comments.stream()
                .map(comment -> {
                    UserShortDto author = authorMap.getOrDefault(
                            comment.getAuthorId(),
                            UserShortDto.builder().id(comment.getAuthorId()).name("[User Unavailable]").build()
                    );
                    return commentMapper.toCommentDto(comment, author);
                })
                .toList();
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, Integer from, Integer size) {
        eventClient.getEventFullById(eventId);

        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED, pageable);

        Map<Long, UserShortDto> authorMap = fetchAuthorsMap(comments);
        return comments.stream()
                .map(comment -> {
                    UserShortDto author = authorMap.getOrDefault(
                            comment.getAuthorId(),
                            UserShortDto.builder().id(comment.getAuthorId()).name("[User Unavailable]").build()
                    );
                    return commentMapper.toCommentDto(comment, author);
                })
                .toList();
    }

    @Override
    @Transactional
    public CommentDto moderateComment(Long commentId, Boolean approve) {
        Comment comment = findById(commentId);

        comment.setStatus(approve ? CommentStatus.PUBLISHED : CommentStatus.REJECTED);
        comment.setUpdated(LocalDateTime.now());
        Comment moderated = commentRepository.save(comment);

        UserShortDto author = userClient.getUserById(moderated.getAuthorId());
        return commentMapper.toCommentDto(comment, author);
    }

    @Override
    public List<CommentDto> getCommentsForModeration(Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findByStatusOrderByCreatedDesc(CommentStatus.PENDING, pageable);

        Map<Long, UserShortDto> authorMap = fetchAuthorsMap(comments);

        return comments.stream()
                .map(comment -> {
                    UserShortDto author = authorMap.getOrDefault(
                            comment.getAuthorId(),
                            UserShortDto.builder().id(comment.getAuthorId()).name("[User Unavailable]").build()
                    );
                    return commentMapper.toCommentDto(comment, author);
                })
                .toList();
    }

    private Comment getCommentByIdAndAuthor(Long commentId, Long authorId) {
        return commentRepository.findByIdAndAuthorId(commentId, authorId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id = %d и authorId = %d не найден".formatted(commentId, authorId)));
    }

    private Map<Long, UserShortDto> fetchAuthorsMap(List<Comment> comments) {
        Set<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (authorIds.isEmpty()) {
            return new HashMap<>();
        }

        List<UserShortDto> authors = userClient.getUsersByIds(new ArrayList<>(authorIds));

        return authors.stream()
                .collect(Collectors.toMap(
                        UserShortDto::getId,
                        Function.identity(),
                        (existing, replacement) -> existing // на случай дубликатов
                ));
    }
}