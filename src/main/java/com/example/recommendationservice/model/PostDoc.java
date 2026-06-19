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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "posts")
public class PostDoc {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String authorId;

    @Field(type = FieldType.Keyword)
    private String visibility;

    @Field(type = FieldType.Text)
    private String text;

    @Field(type = FieldType.Keyword)
    private String postType;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Long)
    private long likesCount;

    @Field(type = FieldType.Long)
    private long commentsCount;

    @Field(type = FieldType.Long)
    private long sharesCount;

    @Field(type = FieldType.Long)
    private long savesCount;

    @Field(type = FieldType.Long)
    private long viewsCount;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Dense_Vector, dims = 1536)
    private float[] embedding;
}