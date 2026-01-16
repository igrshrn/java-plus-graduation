package ru.practicum.mapper;

import ru.practicum.entity.EventSimilarity;
import ru.practicum.entity.embedded.EventSimilarityId;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public class SimilarityMapper {

    public static EventSimilarity toEventSimilarity(EventSimilarityAvro avro) {
        return new EventSimilarity(avro.getEventA(), avro.getEventB(), avro.getScore());
    }

    public static EventSimilarityId toEventSimilarityId(EventSimilarityAvro avro) {
        return new EventSimilarityId(avro.getEventA(), avro.getEventB());
    }
}
