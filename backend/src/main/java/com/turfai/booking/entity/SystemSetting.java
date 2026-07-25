package com.turfai.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * System-wide configuration key-value pair.
 */
@Entity
@Table(name = "system_setting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @NotBlank
    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @NotBlank
    @Column(name = "setting_value", nullable = false, columnDefinition = "text")
    private String value;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemSetting that = (SystemSetting) o;
        return settingKey != null && Objects.equals(settingKey, that.settingKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(settingKey);
    }
}
