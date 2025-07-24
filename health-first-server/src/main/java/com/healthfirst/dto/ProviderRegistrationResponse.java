package com.healthfirst.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderRegistrationResponse {
    private boolean success;
    private String message;
    private ProviderData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderData {
        private UUID providerId;
        private String email;
        private String verificationStatus;
    }

    public static ProviderRegistrationResponse success(UUID providerId, String email, String verificationStatus) {
        ProviderData data = new ProviderData(providerId, email, verificationStatus);
        return new ProviderRegistrationResponse(true, "Provider registered successfully. Account is ready to use.", data);
    }

    public static ProviderRegistrationResponse error(String message) {
        return new ProviderRegistrationResponse(false, message, null);
    }
} 