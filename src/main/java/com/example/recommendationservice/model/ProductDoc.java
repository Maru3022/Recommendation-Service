package com.example.recommendationservice.model;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

@Document(indexName = "products")
@Data
@AllArgsConstructor
public class ProductDoc {

    @Id
    private String id;

    @Field
    private String name;
    private String category;
}
