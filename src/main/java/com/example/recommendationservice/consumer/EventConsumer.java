package com.example.recommendationservice.consumer;

import com.example.recommendationservice.model.UserAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EventConsumer {

    @KafkaListener(topics = "user-actions", groupId = "rec-group")
    public void consumeUserAction(UserAction action){
        log.info("Received action: {] from user: {}", action.getActionType(),action.getUserId());
    }
}
