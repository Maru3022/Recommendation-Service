package com.example.recommendationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI recommendationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Recommendation Service API")
                        .description("A comprehensive recommendation service API that provides personalized product recommendations, popular products, and user action tracking. The service uses collaborative filtering, content-based filtering, and trending algorithms to deliver relevant product suggestions.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Recommendation Service Team")
                                .email("support@recommendation-service.com")
                                .url("https://recommendation-service.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8026")
                                .description("Development server"),
                        new Server()
                                .url("https://api.recommendation-service.com")
                                .description("Production server")
                ));
    }
}
