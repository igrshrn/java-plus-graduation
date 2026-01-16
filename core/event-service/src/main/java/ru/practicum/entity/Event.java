package ru.practicum.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.dto.event.EventState;

import java.time.LocalDateTime;


@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "annotation", nullable = false, length = 2000)
    private String annotation;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "confirmed_requests")
    @Builder.Default
    private Long confirmedRequests = 0L;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "description", length = 7000)
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "initiator_id", nullable = false)
    private Long initiatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "paid", nullable = false)
    @Builder.Default
    private Boolean paid = false;

    @Column(name = "participant_limit")
    @Builder.Default
    private Integer participantLimit = 0;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Column(name = "request_moderation")
    @Builder.Default
    private Boolean requestModeration = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 20)
    @Builder.Default
    private EventState state = EventState.PENDING;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

}