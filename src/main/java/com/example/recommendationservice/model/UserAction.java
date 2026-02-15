package com.example.recommendationservice.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserAction implements Serializable {

    private String userId;
    private String productId;
    private String actionType;

}
