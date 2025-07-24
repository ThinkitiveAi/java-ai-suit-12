package com.healthfirst.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddable class for patient address information
 * Used within the Patient entity for storing residential address
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatientAddress {

    @NotBlank(message = "Street address is required")
    @Size(max = 200, message = "Street address cannot exceed 200 characters")
    @Column(name = "street", length = 200, nullable = false)
    private String street;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "City contains invalid characters")
    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 50, message = "State cannot exceed 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']+$", message = "State contains invalid characters")
    @Column(name = "state", length = 50, nullable = false)
    private String state;

    @NotBlank(message = "ZIP code is required")
    @Pattern(regexp = "^\\d{5}(-\\d{4})?$", message = "Invalid ZIP code format (use XXXXX or XXXXX-XXXX)")
    @Column(name = "zip", length = 10, nullable = false)
    private String zip;

    // Utility methods

    /**
     * Get formatted address string
     */
    public String getFormattedAddress() {
        return String.format("%s, %s, %s %s", street, city, state, zip);
    }

    /**
     * Get city and state
     */
    public String getCityState() {
        return String.format("%s, %s", city, state);
    }

    /**
     * Validate ZIP code format
     */
    public boolean isValidZipCode() {
        if (zip == null) return false;
        return zip.matches("^\\d{5}(-\\d{4})?$");
    }
} 