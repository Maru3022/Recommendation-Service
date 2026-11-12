package com.example.recommendationservice.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class TrainingCreatedEvent {
    private UUID trainingId;
    private UUID userId;
    private LocalDate trainingDate;
    private String trainingName;
}
