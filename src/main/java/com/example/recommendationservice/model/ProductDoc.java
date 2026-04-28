package com.example.recommendationservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Document(indexName = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductDoc {

    @Id
    @NotBlank(message = "Product ID cannot be blank")
    private String id;

    @Field(type = FieldType.Text)
    @NotBlank(message = "Product name cannot be blank")
    private String name;

    @Field(type = FieldType.Keyword)
    @NotBlank(message = "Product category cannot be blank")
    private String category;

    @NotNull(message = "Product price cannot be null")
    @Positive(message = "Product price must be positive")
    private Double price;

    private String imageUrl;
}
