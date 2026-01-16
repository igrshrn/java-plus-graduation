package ru.practicum.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.deserializer.EventSimilarityAvroDeserializer;
import ru.practicum.deserializer.UserActionAvroDeserializer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Properties;

@Configuration("ru.practicum.config")
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaSettingsConfig kafkaSettingsConfig;

    @Bean
    public KafkaConsumer<String, EventSimilarityAvro> eventSimilarityAvroKafkaConsumer() {

        Properties config = new Properties();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaSettingsConfig.getBootstrapAddress());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSimilarityAvroDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "similarity-cg");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new KafkaConsumer<>(config);
    }

    @Bean
    public KafkaConsumer<String, UserActionAvro> userActionAvroKafkaConsumer() {

        Properties config = new Properties();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaSettingsConfig.getBootstrapAddress());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionAvroDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "user-action-cg");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new KafkaConsumer<>(config);
    }
}