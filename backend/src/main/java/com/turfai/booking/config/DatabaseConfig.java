package com.turfai.booking.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Robust Database Configuration for Render & Neon PostgreSQL deployment.
 * Automatically normalizes PostgreSQL URLs (e.g. postgresql:// or postgres:// -> jdbc:postgresql://)
 * and parses embedded credentials if supplied via DATABASE_URL or SPRING_DATASOURCE_URL.
 */
@Slf4j
@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    public DataSource dataSource(
            DataSourceProperties properties,
            @Value("${spring.datasource.hikari.maximum-pool-size:10}") int maxPoolSize,
            @Value("${spring.datasource.hikari.minimum-idle:2}") int minIdle,
            @Value("${spring.datasource.hikari.connection-timeout:30000}") long connectionTimeout,
            @Value("${spring.datasource.hikari.idle-timeout:600000}") long idleTimeout,
            @Value("${spring.datasource.hikari.max-lifetime:1800000}") long maxLifetime,
            @Value("${spring.datasource.hikari.pool-name:turfai-pool}") String poolName) {

        String rawUrl = properties.getUrl();
        String username = properties.getUsername();
        String password = properties.getPassword();

        NormalizedJdbcDetails normalized = normalizeDatabaseUrl(rawUrl, username, password);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(normalized.jdbcUrl());
        hikariConfig.setUsername(normalized.username());
        hikariConfig.setPassword(normalized.password());
        String driverClassName = properties.getDriverClassName();
        if (driverClassName == null || driverClassName.isBlank()) {
            driverClassName = normalized.jdbcUrl().startsWith("jdbc:h2:") ? "org.h2.Driver" : "org.postgresql.Driver";
        }
        hikariConfig.setDriverClassName(driverClassName);

        // Connection pool tuning parameters
        hikariConfig.setMaximumPoolSize(maxPoolSize);
        hikariConfig.setMinimumIdle(minIdle);
        hikariConfig.setConnectionTimeout(connectionTimeout);
        hikariConfig.setIdleTimeout(idleTimeout);
        hikariConfig.setMaxLifetime(maxLifetime);
        hikariConfig.setPoolName(poolName);

        // Neon PostgreSQL optimizations
        hikariConfig.addDataSourceProperty("reWriteBatchedInserts", "true");
        hikariConfig.addDataSourceProperty("tcpKeepAlive", "true");

        log.info("HikariDataSource initialized successfully for JDBC URL: {}", maskJdbcUrl(normalized.jdbcUrl()));
        return new HikariDataSource(hikariConfig);
    }

    public static NormalizedJdbcDetails normalizeDatabaseUrl(String rawUrl, String defaultUser, String defaultPass) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return new NormalizedJdbcDetails("jdbc:postgresql://localhost:5432/turfai", defaultUser, defaultPass);
        }

        String trimmed = rawUrl.trim();
        String finalUrl = trimmed;
        String finalUser = defaultUser;
        String finalPass = defaultPass;

        if (trimmed.startsWith("postgres://") || trimmed.startsWith("postgresql://")) {
            try {
                String uriString = trimmed.startsWith("postgres://") ?
                        "http" + trimmed.substring(8) : "http" + trimmed.substring(10);
                URI uri = new URI(uriString);

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    finalUser = userInfo[0];
                    if (userInfo.length > 1) {
                        finalPass = userInfo[1];
                    }
                }

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String query = uri.getQuery();

                StringBuilder jdbcBuilder = new StringBuilder("jdbc:postgresql://");
                jdbcBuilder.append(host).append(":").append(port).append(path);

                if (query != null && !query.isBlank()) {
                    jdbcBuilder.append("?").append(query);
                } else {
                    jdbcBuilder.append("?sslmode=require");
                }

                finalUrl = jdbcBuilder.toString();
            } catch (Exception e) {
                if (trimmed.startsWith("postgres://")) {
                    finalUrl = "jdbc:postgresql://" + trimmed.substring(11);
                } else if (trimmed.startsWith("postgresql://")) {
                    finalUrl = "jdbc:postgresql://" + trimmed.substring(13);
                }
            }
        } else if (!trimmed.startsWith("jdbc:")) {
            finalUrl = "jdbc:" + trimmed;
        }

        return new NormalizedJdbcDetails(finalUrl, finalUser, finalPass);
    }

    public record NormalizedJdbcDetails(String jdbcUrl, String username, String password) {}

    private String maskJdbcUrl(String url) {
        if (url == null) return "null";
        return url.replaceAll(":[^/@]+@", ":****@");
    }
}
