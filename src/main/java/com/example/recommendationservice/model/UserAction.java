package com.example.recommendationservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "user_actions")
public class UserAction implements Serializable {

    @Id
    @NotBlank(message = "Action ID cannot be blank")
    private String id;

    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    @NotBlank(message = "Product ID cannot be blank")
    private String productId;
    
    @NotBlank(message = "Action type cannot be blank")
    private String actionType;

}
