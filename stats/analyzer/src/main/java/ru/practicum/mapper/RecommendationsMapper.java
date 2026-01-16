package ru.practicum.mapper;

import ru.practicum.ewm.stats.proto.RecommendedEventProto;

public class RecommendationsMapper {

    public static RecommendedEventProto toRecommendedEventProto(Long eventId, Double score) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(score)
                .build();
    }
}
