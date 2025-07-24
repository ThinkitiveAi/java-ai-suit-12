package com.healthfirst.config;

import com.healthfirst.filter.JwtAuthenticationFilter;
import com.healthfirst.middleware.RateLimitingMiddleware;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RateLimitingMiddleware rateLimitingMiddleware;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/api/v1/provider/register",
                    "/api/v1/provider/verify",
                    "/api/v1/provider/resend-verification",
                    "/api/v1/provider/health",
                    "/api/v1/provider/login",
                    "/api/v1/provider/refresh",
                    "/api/v1/patient/register",
                    "/api/v1/patient/verify",
                    "/api/v1/patient/verify-phone",
                    "/api/v1/patient/resend-verification",
                    "/api/v1/patient/resend-phone-verification",
                    "/api/v1/patient/health",
                    "/api/v1/patient/check-email",
                    "/api/v1/patient/check-phone",
                    "/api/v1/patient/login",
                    "/api/v1/patient/refresh"
                ).permitAll()
                
                // H2 Console access (development only)
                .requestMatchers("/h2-console/**").permitAll()
                
                // Swagger/OpenAPI endpoints
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/v3/api-docs/**"
                ).permitAll()
                
                // Actuator endpoints
                .requestMatchers("/actuator/health").permitAll()
                
                // Error pages
                .requestMatchers("/error").permitAll()
                
                // Admin endpoints - require authentication
                .requestMatchers("/api/v1/provider/stats/**").hasRole("ADMIN")
                
                // Protected endpoints - require authentication
                .requestMatchers(
                    "/api/v1/provider/logout",
                    "/api/v1/provider/logout-all",
                    "/api/v1/provider/sessions",
                    "/api/v1/provider/sessions/**",
                    "/api/v1/provider/auth/**",
                    "/api/v1/provider/profile/**",
                    "/api/v1/provider/availability",
                    "/api/v1/provider/availability/**",
                    "/api/v1/patient/logout",
                    "/api/v1/patient/logout-all",
                    "/api/v1/patient/sessions",
                    "/api/v1/patient/sessions/**",
                    "/api/v1/patient/auth/**"
                ).authenticated()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                // Allow frames for H2 console
                .frameOptions().sameOrigin()
            )
            // Add rate limiting filter before authentication
                                    .addFilterBefore(rateLimitingMiddleware, UsernamePasswordAuthenticationFilter.class)
                        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins in production
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // 12 rounds for security
    }
} 