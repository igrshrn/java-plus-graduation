package ru.practicum.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.entity.embedded.UserActionId;

import java.time.Instant;

@Entity
@Table(name = "user_actions")
@IdClass(UserActionId.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserAction {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "user_score")
    private Double score;

    @Column(name = "timestamp_action")
    private Instant timestamp;
}
