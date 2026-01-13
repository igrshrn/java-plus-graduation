package ru.practicum.client.user;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.user.UserShortDto;

import java.util.List;

@FeignClient(
        name = "user-service",
        path = "/internal/users",
        fallback = UserClientFallback.class
)
public interface UserClient {
    @GetMapping("/{id}")
    UserShortDto getUserById(@PathVariable Long id);

    @PostMapping("/batch")
    List<UserShortDto> getUsersByIds(@RequestBody List<Long> ids);
}
