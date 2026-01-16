package ru.practicum.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@RequiredArgsConstructor
@ConfigurationProperties("kafka")
public class KafkaSettingsConfig {

    private final String bootstrapAddress;
    private final String topic;

}
