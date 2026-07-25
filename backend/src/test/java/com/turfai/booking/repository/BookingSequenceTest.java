package com.turfai.booking.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BookingSequenceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("booking_number_seq sequence should exist and generate monotonic numbers")
    void testBookingNumberSequence() {
        Long nextVal1 = jdbcTemplate.queryForObject("SELECT NEXTVAL('booking_number_seq')", Long.class);
        Long nextVal2 = jdbcTemplate.queryForObject("SELECT NEXTVAL('booking_number_seq')", Long.class);

        assertThat(nextVal1).isNotNull();
        assertThat(nextVal2).isNotNull();
        assertThat(nextVal2).isEqualTo(nextVal1 + 1);
    }
}
