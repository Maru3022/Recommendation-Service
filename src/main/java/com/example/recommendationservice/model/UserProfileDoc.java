package com.example.recommendationservice.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Data
@NoArgsConstructor
@Document(indexName = "user_profiles")
public class UserProfileDoc {

    @Id
    private String userId;

    @Field(type = FieldType.Dense_Vector, dims = 1536)
    private float[] interestEmbedding;

    @Field(type = FieldType.Date)
    private Instant updatedAt;
}
