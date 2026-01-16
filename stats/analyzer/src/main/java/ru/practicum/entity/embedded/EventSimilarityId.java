package ru.practicum.entity.embedded;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventSimilarityId implements Serializable {
    private Long first;
    private Long second;
}
