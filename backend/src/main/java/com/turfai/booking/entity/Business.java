package com.turfai.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a Turf Business tenant.
 *
 * <p>Each business has a dedicated WhatsApp Business phone number for webhook routing (ADR-006)
 * and operates in a specific IANA timezone (ADR-019).
 */
@Entity
@Table(name = "business")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business extends BaseEntity {

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Column(name = "google_maps_link", columnDefinition = "text")
    private String googleMapsLink;

    @Column(name = "phone", length = 20)
    private String phone;

    @NotBlank
    @Column(name = "whatsapp_phone_number_id", nullable = false, unique = true, length = 100)
    private String whatsappPhoneNumberId;

    @NotBlank
    @Builder.Default
    @Column(name = "timezone", nullable = false, length = 100)
    private String timezone = "Asia/Kolkata";

    @NotNull
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private BusinessStatus status = BusinessStatus.ACTIVE;
}
