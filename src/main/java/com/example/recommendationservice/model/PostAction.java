package com.example.recommendationservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@Entity
@Table(name = "post_actions",
        indexes = {
                @Index(name = "idx_action_user_post", columnList = "userId, postId"),
                @Index(name = "idx_action_post", columnList = "postId")
        })
public class PostAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String postId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    private double weight;

    private Instant createdAt;

    public enum ActionType {
        LIKE, COMMENT, SAVE, VIEW, SHARE, SKIP;

        public double defaultWeight() {
            return switch (this) {
                case LIKE -> 1.0;
                case COMMENT -> 1.2;
                case SAVE -> 1.5;
                case SHARE -> 1.3;
                case VIEW -> 0.1;
                case SKIP -> -0.3;
            };
        }
    }
}