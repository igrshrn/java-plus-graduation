package ru.practicum.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.practicum.config.KafkaSettingsConfig;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.mapper.UserActionMapper;

@Service
@RequiredArgsConstructor
public class UserActionHandler implements CollectorHandler<UserActionProto> {

    private final KafkaSettingsConfig kafkaSettingsConfig;
    private final Producer<String, SpecificRecordBase> producer;

    public void handle(UserActionProto proto) {
        producer.send(new ProducerRecord<>(kafkaSettingsConfig.getTopic(), UserActionMapper.mapToAvro(proto)));
    }
}
