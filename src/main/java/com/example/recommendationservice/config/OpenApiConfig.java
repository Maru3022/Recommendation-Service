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
                        .description("Personalized fitness post feed service. Provides hybrid-ranked feeds, " +
                                "social graph management, post lifecycle, semantic (RAG) search, " +
                                "and user-interaction tracking for the fitness app.")
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Fitness App Team")
                                .email("support@fitness-app.example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8026").description("Development server"),
                        new Server().url("https://api.fitness-app.example.com").description("Production server")
                ));
    }
}
