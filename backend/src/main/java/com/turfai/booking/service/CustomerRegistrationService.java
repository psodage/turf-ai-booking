package com.turfai.booking.service;

import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.entity.UserStatus;
import com.turfai.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerRegistrationService {

    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateCustomer(String phone, String name) {
        String formattedPhone = phone.startsWith("+") ? phone : "+" + phone;
        return userRepository.findByPhone(formattedPhone)
                .orElseGet(() -> {
                    log.info("Auto-registering new customer user for phone {}", formattedPhone);
                    User newCustomer = User.builder()
                            .phone(formattedPhone)
                            .name(name != null && !name.isBlank() ? name : formattedPhone)
                            .role(UserRole.CUSTOMER)
                            .business(null) // ADR-002: Customer business_id is null
                            .status(UserStatus.ACTIVE)
                            .language("en")
                            .build();
                    return userRepository.save(newCustomer);
                });
    }
}
