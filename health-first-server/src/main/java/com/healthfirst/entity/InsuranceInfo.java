package com.healthfirst.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddable class for patient insurance information
 * Optional field within the Patient entity with privacy considerations
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceInfo {

    @Size(max = 100, message = "Insurance provider name cannot exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-&'.,]*$", message = "Insurance provider contains invalid characters")
    @Column(name = "provider", length = 100)
    private String provider;

    @Size(max = 50, message = "Policy number cannot exceed 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\-]*$", message = "Policy number contains invalid characters")
    @Column(name = "policy_number", length = 50)
    private String policyNumber;

    @Size(max = 50, message = "Group number cannot exceed 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\-]*$", message = "Group number contains invalid characters")
    @Column(name = "group_number", length = 50)
    private String groupNumber;

    @Size(max = 100, message = "Plan name cannot exceed 100 characters")
    @Column(name = "plan_name", length = 100)
    private String planName;

    @Size(max = 100, message = "Member ID cannot exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\-]*$", message = "Member ID contains invalid characters")
    @Column(name = "member_id", length = 100)
    private String memberId;

    // Utility methods

    /**
     * Check if insurance information is complete
     */
    public boolean isComplete() {
        return provider != null && !provider.trim().isEmpty() &&
               policyNumber != null && !policyNumber.trim().isEmpty();
    }

    /**
     * Check if insurance information has any data
     */
    public boolean hasAnyInformation() {
        return (provider != null && !provider.trim().isEmpty()) ||
               (policyNumber != null && !policyNumber.trim().isEmpty()) ||
               (groupNumber != null && !groupNumber.trim().isEmpty()) ||
               (planName != null && !planName.trim().isEmpty()) ||
               (memberId != null && !memberId.trim().isEmpty());
    }

    /**
     * Get masked policy number for display (HIPAA compliance)
     */
    public String getMaskedPolicyNumber() {
        if (policyNumber == null || policyNumber.trim().isEmpty()) {
            return null;
        }
        if (policyNumber.length() <= 4) {
            return "****";
        }
        // Show last 4 characters only
        return "****" + policyNumber.substring(policyNumber.length() - 4);
    }

    /**
     * Get masked member ID for display (HIPAA compliance)
     */
    public String getMaskedMemberId() {
        if (memberId == null || memberId.trim().isEmpty()) {
            return null;
        }
        if (memberId.length() <= 4) {
            return "****";
        }
        // Show last 4 characters only
        return "****" + memberId.substring(memberId.length() - 4);
    }

    /**
     * Get formatted insurance information for display
     */
    public String getFormattedInsurance() {
        if (!isComplete()) {
            return "Incomplete insurance information";
        }
        
        StringBuilder formatted = new StringBuilder();
        formatted.append(provider);
        
        if (planName != null && !planName.trim().isEmpty()) {
            formatted.append(" - ").append(planName);
        }
        
        formatted.append(" (Policy: ").append(getMaskedPolicyNumber()).append(")");
        
        return formatted.toString();
    }

    /**
     * Clear all insurance information
     */
    public void clear() {
        this.provider = null;
        this.policyNumber = null;
        this.groupNumber = null;
        this.planName = null;
        this.memberId = null;
    }

    /**
     * Validate policy number format (basic validation)
     */
    public boolean isValidPolicyNumber() {
        if (policyNumber == null || policyNumber.trim().isEmpty()) {
            return true; // Optional field
        }
        // Basic validation - alphanumeric and dashes only
        return policyNumber.matches("^[a-zA-Z0-9\\-]+$");
    }

    /**
     * Common insurance providers for validation/suggestions
     */
    public enum CommonProvider {
        BLUE_CROSS("Blue Cross Blue Shield"),
        AETNA("Aetna"),
        ANTHEM("Anthem"),
        CIGNA("Cigna"),
        HUMANA("Humana"),
        UNITED_HEALTHCARE("UnitedHealthcare"),
        KAISER("Kaiser Permanente"),
        MEDICARE("Medicare"),
        MEDICAID("Medicaid"),
        TRICARE("TRICARE"),
        OTHER("Other");

        private final String displayName;

        CommonProvider(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static boolean isKnownProvider(String provider) {
            if (provider == null) return false;
            for (CommonProvider prov : values()) {
                if (prov.displayName.equalsIgnoreCase(provider.trim())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Encrypt sensitive fields for storage (placeholder for actual encryption)
     * In production, this should use proper encryption libraries
     */
    public void encryptSensitiveData() {
        // TODO: Implement proper encryption for HIPAA compliance
        // This is a placeholder - in production, use proper encryption
        // like AES encryption for policy numbers and member IDs
    }

    /**
     * Decrypt sensitive fields for use (placeholder for actual decryption)
     */
    public void decryptSensitiveData() {
        // TODO: Implement proper decryption for HIPAA compliance
        // This is a placeholder - in production, use proper decryption
    }
} 