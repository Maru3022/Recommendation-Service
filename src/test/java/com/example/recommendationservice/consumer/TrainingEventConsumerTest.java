package com.example.recommendationservice.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TrainingEventConsumerTest {
    @Test void planningEventsAreNotPublicationCommands() {
        boolean consumesPlanningEvents = Arrays.stream(TrainingEventConsumer.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(KafkaListener.class))
                .filter(java.util.Objects::nonNull)
                .flatMap(listener -> Arrays.stream(listener.topics()))
                .anyMatch("training.created"::equals);
        assertFalse(consumesPlanningEvents);
    }
}
