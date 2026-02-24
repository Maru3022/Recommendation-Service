package com.example.recommendationservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "user_actions")
public class UserAction implements Serializable {

    @Id
    private String id;

    private String userId;
    private String productId;
    private String actionType;

}
