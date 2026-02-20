package com.example.recommendationservice.consumer;

import com.example.recommendationservice.model.UserAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EventConsumer {

    @KafkaListener(
            topics = "user-actions",
            groupId = "rec-group",
            concurrency = "3"
    )
    public void consumeUserAction(UserAction action){
        log.info("Processing action: {} for user: {}", action.getActionType(), action.getUserId());
    }
}