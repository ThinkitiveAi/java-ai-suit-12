package com.healthfirst.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddable class for patient emergency contact information
 * Optional field within the Patient entity
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

    @Size(max = 100, message = "Emergency contact name cannot exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']*$", message = "Emergency contact name contains invalid characters")
    @Column(name = "name", length = 100)
    private String name;

    @Pattern(regexp = "^(\\+?[1-9]\\d{1,14})?$", message = "Invalid emergency contact phone number format")
    @Column(name = "phone", length = 20)
    private String phone;

    @Size(max = 50, message = "Relationship cannot exceed 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-']*$", message = "Relationship contains invalid characters")
    @Column(name = "relationship", length = 50)
    private String relationship;

    // Utility methods

    /**
     * Check if emergency contact has all required information
     */
    public boolean isComplete() {
        return name != null && !name.trim().isEmpty() &&
               phone != null && !phone.trim().isEmpty() &&
               relationship != null && !relationship.trim().isEmpty();
    }

    /**
     * Check if emergency contact has any information
     */
    public boolean hasAnyInformation() {
        return (name != null && !name.trim().isEmpty()) ||
               (phone != null && !phone.trim().isEmpty()) ||
               (relationship != null && !relationship.trim().isEmpty());
    }

    /**
     * Get formatted emergency contact string
     */
    public String getFormattedContact() {
        if (!isComplete()) {
            return "Incomplete emergency contact information";
        }
        return String.format("%s (%s) - %s", name, relationship, phone);
    }

    /**
     * Validate phone number format
     */
    public boolean isValidPhoneNumber() {
        if (phone == null || phone.trim().isEmpty()) {
            return true; // Optional field
        }
        return phone.matches("^\\+?[1-9]\\d{1,14}$");
    }

    /**
     * Clear all emergency contact information
     */
    public void clear() {
        this.name = null;
        this.phone = null;
        this.relationship = null;
    }

    /**
     * Common relationships for validation/suggestions
     */
    public enum CommonRelationship {
        SPOUSE("Spouse"),
        PARENT("Parent"),
        CHILD("Child"),
        SIBLING("Sibling"),
        FRIEND("Friend"),
        GUARDIAN("Guardian"),
        CAREGIVER("Caregiver"),
        OTHER("Other");

        private final String displayName;

        CommonRelationship(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static boolean isValidRelationship(String relationship) {
            if (relationship == null) return false;
            for (CommonRelationship rel : values()) {
                if (rel.displayName.equalsIgnoreCase(relationship.trim())) {
                    return true;
                }
            }
            return false;
        }
    }
} 