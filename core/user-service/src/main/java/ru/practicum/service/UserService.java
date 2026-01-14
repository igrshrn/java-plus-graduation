package ru.practicum.service;

import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.User;

import java.util.List;

public interface UserService {

    UserDto create(NewUserRequest request);

    List<UserDto> getAll(List<Long> ids, int from, int size);

    void delete(Long id);

    User getById(Long userId);

    UserShortDto getUserShort(Long id);

    List<UserShortDto> getUsersShort(List<Long> ids);
}
