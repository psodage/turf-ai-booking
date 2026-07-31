package com.turfai.booking.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Test
    @DisplayName("Flyway should successfully execute all core migrations V1-V17 and seed data")
    void testFlywayMigrationsAndSeedData() {
        // Verify all 16 core domain tables exist in the database schema
        List<String> expectedTables = List.of(
                "business",
                "users",
                "turf",
                "operating_hours",
                "pricing_rule",
                "booking",
                "booking_hold",
                "payment",
                "blocked_slot",
                "conversation",
                "conversation_message",
                "notification",
                "booking_audit",
                "payment_audit",
                "report",
                "system_setting"
        );

        for (String tableName : expectedTables) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE UPPER(table_name) = ?",
                    Integer.class,
                    tableName.toUpperCase()
            );
            assertThat(count)
                    .withFailMessage("Table '%s' should exist after Flyway migrations", tableName)
                    .isGreaterThanOrEqualTo(1);
        }

        // Verify Seed Data (R__seed_demo_data.sql)
        assertThat(businessRepository.findByWhatsappPhoneNumberId("1174774225727644")).isPresent();
        assertThat(userRepository.findByPhone("+919325025671")).isPresent();

        // Verify System Settings seed
        assertThat(systemSettingRepository.findById("HOLD_DURATION_MINUTES")).isPresent();
        assertThat(systemSettingRepository.findById("HOLD_DURATION_MINUTES").get().getValue()).isEqualTo("7.5");
        assertThat(systemSettingRepository.findById("CANCELLATION_WINDOW_HOURS").get().getValue()).isEqualTo("2");
    }
}
