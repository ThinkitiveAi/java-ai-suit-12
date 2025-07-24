package com.healthfirst.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 3 configuration for Healthcare Management System API
 * Provides comprehensive API documentation with security schemes
 */
@Configuration
public class SwaggerConfig {

    @Value("${app.swagger.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${app.swagger.prod-url:https://api.healthfirst.com}")
    private String prodUrl;

    @Bean
    public OpenAPI openAPI() {
        // Define servers
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Development Server");

        Server prodServer = new Server();
        prodServer.setUrl(prodUrl);
        prodServer.setDescription("Production Server");

        // Define contact information
        Contact contact = new Contact();
        contact.setEmail("support@healthfirst.com");
        contact.setName("HealthFirst Support Team");
        contact.setUrl("https://healthfirst.com/support");

        // Define license
        License license = new License()
                .name("MIT License")
                .url("https://choosealicense.com/licenses/mit/");

        // Define API info
        Info info = new Info()
                .title("HealthFirst - Healthcare Management System API")
                .version("1.0.0")
                .contact(contact)
                .description("""
                        Comprehensive Healthcare Management System API providing secure endpoints for:
                        
                        ## Features:
                        - **Provider Management**: Registration, authentication, and profile management
                        - **Patient Management**: Registration, authentication, and health records
                        - **Availability Management**: Provider scheduling and appointment slots
                        - **Security**: JWT-based authentication with role-based access control
                        - **Compliance**: HIPAA-compliant data handling and audit trails
                        - **Rate Limiting**: API protection against abuse
                        
                        ## Authentication:
                        This API uses JWT (JSON Web Tokens) for authentication. Include the token in the Authorization header:
                        ```
                        Authorization: Bearer <your-jwt-token>
                        ```
                        
                        ## User Types:
                        - **Providers**: Healthcare professionals (doctors, nurses, etc.)
                        - **Patients**: Healthcare consumers
                        - **Admins**: System administrators
                        
                        ## Security & Privacy:
                        - All patient data is handled in compliance with HIPAA regulations
                        - Passwords are encrypted using BCrypt with 12 salt rounds
                        - API endpoints are rate-limited to prevent abuse
                        - Comprehensive audit logging for security monitoring
                        """)
                .license(license);

        // Define security requirement
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Bearer Authentication");

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer, prodServer))
                .addSecurityItem(securityRequirement)
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        JWT token for API authentication.
                        
                        ### How to obtain a token:
                        1. **Provider Login**: POST /api/v1/provider/login
                        2. **Patient Login**: POST /api/v1/patient/login
                        
                        ### Token Format:
                        ```
                        Authorization: Bearer <jwt-token>
                        ```
                        
                        ### Token Claims:
                        - **Provider tokens**: Include role, verification status, provider ID
                        - **Patient tokens**: Include age category, verification status, patient ID
                        
                        ### Token Expiry:
                        - **Access tokens**: 1 hour (configurable)
                        - **Refresh tokens**: 30 days (configurable)
                        """);
    }
} 