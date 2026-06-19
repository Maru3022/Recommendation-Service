package com.example.recommendationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.List;

@Document(indexName = "posts")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDoc {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String authorId;
    private String authorDisplayName;
    private String authorAvatarUrl;

    @Field(type = FieldType.Text)
    private String text;
    private List<String> mediaUrls;
    @Field(type = FieldType.Keyword)
    private String postType;
    private List<String> tags;
    @Field(type = FieldType.Keyword)
    private String category;

    private String relatedTrainingId;
    private Double durationMinutes;
    private Double caloriesBurned;

    private long likesCount;
    private long commentsCount;
    private long viewsCount;

    private Instant createdAt;
    @Field(type = FieldType.Keyword)
    private String visibility;

    @Field(type = FieldType.Dense_Vector, dims = 1536)
    private float[] embedding;
}
