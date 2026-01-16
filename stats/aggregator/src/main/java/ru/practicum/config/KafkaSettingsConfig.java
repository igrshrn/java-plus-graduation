package ru.practicum.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "kafka")
public class KafkaSettingsConfig {

    private final String bootstrapAddress;
    private final Topics topics;

    @Getter
    @RequiredArgsConstructor
    public static class Topics {
        private final String userAction;
        private final String eventsSimilarity;
    }
}