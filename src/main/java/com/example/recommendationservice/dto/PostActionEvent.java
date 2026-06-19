package com.example.recommendationservice.dto;

import com.example.recommendationservice.model.PostAction;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class PostActionEvent {
    private String userId;
    private String postId;
    private PostAction.ActionType actionType;
    private Instant timestamp;
}