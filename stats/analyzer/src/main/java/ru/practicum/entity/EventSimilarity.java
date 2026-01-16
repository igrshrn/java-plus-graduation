package ru.practicum.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.entity.embedded.EventSimilarityId;

@Entity
@Table(name = "event_similarities")
@IdClass(EventSimilarityId.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventSimilarity {

    @Id
    @Column(name = "first_event")
    private Long first;

    @Id
    @Column(name = "second_event")
    private Long second;

    private Double score;
}
