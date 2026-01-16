package ru.practicum.mapper;

import lombok.NoArgsConstructor;
import ru.practicum.entity.UserAction;
import ru.practicum.entity.embedded.UserActionId;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.util.WeightConverter;

@NoArgsConstructor
public class UserActionMapper {

    public static UserAction toUserAction(UserActionAvro avro) {
        return new UserAction(avro.getUserId(), avro.getEventId(),
                WeightConverter.getWeightOnAction(avro.getActionType()), avro.getTimestamp());
    }

    public static UserActionId toUserActionId(UserActionAvro avro) {
        return new UserActionId(avro.getUserId(), avro.getEventId());
    }
}
