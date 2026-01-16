package ru.practicum.config;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ewm.deserializer.UserActionAvroDeserializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ewm.serializer.AvroSerializer;

import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaSettingsConfig kafkaSettingsConfig;

    @Bean
    public KafkaConsumer<String, UserActionAvro> kafkaConsumer() {

        Properties config = new Properties();
        config.put(BOOTSTRAP_SERVERS_CONFIG, kafkaSettingsConfig.getBootstrapAddress());
        config.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(VALUE_DESERIALIZER_CLASS_CONFIG, UserActionAvroDeserializer.class);
        config.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "aggregators-stat");

        return new KafkaConsumer<>(config);
    }

    @Bean
    public Producer<String, SpecificRecordBase> producer() {

        Properties config = new Properties();
        config.put(BOOTSTRAP_SERVERS_CONFIG, kafkaSettingsConfig.getBootstrapAddress());
        config.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class);

        return new KafkaProducer<>(config);
    }
}