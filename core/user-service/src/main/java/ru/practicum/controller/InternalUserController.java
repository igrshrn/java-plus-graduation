package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.client.user.UserClient;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController implements UserClient {
    private final UserService userService;

    public UserShortDto getUserById(Long id) {
        return userService.getUserShort(id);
    }

    public List<UserShortDto> getUsersByIds(List<Long> ids) {
        return userService.getUsersShort(ids);
    }
}
