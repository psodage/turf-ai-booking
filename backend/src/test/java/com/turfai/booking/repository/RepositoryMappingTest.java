package com.turfai.booking.repository;

import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.BusinessStatus;
import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryMappingTest {

    @Autowired
    private BusinessRepository businessRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should persist and retrieve Business and User entities cleanly")
    void testBusinessAndUserMapping() {
        Business business = Business.builder()
                .name("Test Turf Kolhapur")
                .whatsappPhoneNumberId("PN_TEST_999")
                .timezone("Asia/Kolkata")
                .status(BusinessStatus.ACTIVE)
                .build();

        Business savedBusiness = businessRepository.save(business);
        assertThat(savedBusiness.getId()).isNotNull();
        assertThat(savedBusiness.getCreatedAt()).isNotNull();

        User customer = User.builder()
                .name("Rohan Sharma")
                .phone("+919999988888")
                .role(UserRole.CUSTOMER)
                .business(null) // ADR-002: Customer business_id is null
                .status(UserStatus.ACTIVE)
                .build();

        User savedCustomer = userRepository.save(customer);
        assertThat(savedCustomer.getId()).isNotNull();
        assertThat(savedCustomer.getBusiness()).isNull();

        User owner = User.builder()
                .name("Owner Patil")
                .phone("+919999977777")
                .role(UserRole.OWNER)
                .business(savedBusiness) // ADR-002: Owner must be linked to business
                .status(UserStatus.ACTIVE)
                .build();

        User savedOwner = userRepository.save(owner);
        assertThat(savedOwner.getId()).isNotNull();
        assertThat(savedOwner.getBusiness().getId()).isEqualTo(savedBusiness.getId());

        Optional<Business> foundBusiness = businessRepository.findByWhatsappPhoneNumberId("PN_TEST_999");
        assertThat(foundBusiness).isPresent();
        assertThat(foundBusiness.get().getName()).isEqualTo("Test Turf Kolhapur");
    }
}
