package ru.practicum.entity.embedded;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserActionId implements Serializable {
    private Long userId;
    private Long eventId;
}
