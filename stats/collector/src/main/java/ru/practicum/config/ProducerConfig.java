package ru.practicum.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.serializer.AvroSerializer;

import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@Configuration
@RequiredArgsConstructor
public class ProducerConfig {

    private final KafkaSettingsConfig kafkaSettingsConfig;

    @Bean
    public Producer<String, SpecificRecordBase> producer() {
        Properties config = new Properties();
        config.put(BOOTSTRAP_SERVERS_CONFIG, kafkaSettingsConfig.getBootstrapAddress());
        config.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class);

        return new KafkaProducer<>(config);
    }
}