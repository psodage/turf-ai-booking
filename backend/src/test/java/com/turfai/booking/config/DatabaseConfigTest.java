package com.turfai.booking.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConfigTest {

    @Test
    @DisplayName("Should convert postgresql:// URI without jdbc: prefix to valid jdbc:postgresql:// URL")
    void testNormalizeRawPostgresqlUrlWithoutCredentials() {
        String rawUrl = "postgresql://ep-xyz-123.eastus2.azure.neon.tech/neondb?sslmode=require";

        DatabaseConfig.NormalizedJdbcDetails result = DatabaseConfig.normalizeDatabaseUrl(rawUrl, "default_user", "default_pass");

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://ep-xyz-123.eastus2.azure.neon.tech:5432/neondb?sslmode=require");
        assertThat(result.username()).isEqualTo("default_user");
        assertThat(result.password()).isEqualTo("default_pass");
    }

    @Test
    @DisplayName("Should parse embedded username and password from postgresql:// URI")
    void testNormalizePostgresqlUrlWithEmbeddedCredentials() {
        String rawUrl = "postgresql://neondb_owner:SecretPassword123@ep-xyz-123.eastus2.azure.neon.tech/neondb?sslmode=require";

        DatabaseConfig.NormalizedJdbcDetails result = DatabaseConfig.normalizeDatabaseUrl(rawUrl, "default_user", "default_pass");

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://ep-xyz-123.eastus2.azure.neon.tech:5432/neondb?sslmode=require");
        assertThat(result.username()).isEqualTo("neondb_owner");
        assertThat(result.password()).isEqualTo("SecretPassword123");
    }

    @Test
    @DisplayName("Should parse postgres:// URI format")
    void testNormalizePostgresSchemeUrl() {
        String rawUrl = "postgres://admin:pass@db.example.com:5433/mydb";

        DatabaseConfig.NormalizedJdbcDetails result = DatabaseConfig.normalizeDatabaseUrl(rawUrl, "default_user", "default_pass");

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://db.example.com:5433/mydb?sslmode=require");
        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.password()).isEqualTo("pass");
    }

    @Test
    @DisplayName("Should leave already valid jdbc:postgresql:// URL unchanged")
    void testStandardJdbcUrlUnchanged() {
        String rawUrl = "jdbc:postgresql://localhost:5432/turfai";

        DatabaseConfig.NormalizedJdbcDetails result = DatabaseConfig.normalizeDatabaseUrl(rawUrl, "my_user", "my_pass");

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/turfai");
        assertThat(result.username()).isEqualTo("my_user");
        assertThat(result.password()).isEqualTo("my_pass");
    }
}
