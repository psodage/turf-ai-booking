package com.turfai.booking.service;

import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.entity.UserStatus;
import com.turfai.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerRegistrationService {

    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateCustomer(String phone, String name) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }

        String digitsOnly = phone.replaceAll("[^0-9]", "");
        String last10 = digitsOnly.length() >= 10 ? digitsOnly.substring(digitsOnly.length() - 10) : digitsOnly;
        String formattedPhone = phone.startsWith("+") ? phone : "+" + phone;

        // 1. Try exact match by formatted phone or raw phone
        Optional<User> userOpt = userRepository.findByPhone(formattedPhone);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhone(phone);
        }

        // 2. Try flexible match by last 10 digits
        if (userOpt.isEmpty() && !last10.isEmpty()) {
            List<User> matches = userRepository.findByPhoneEndingWith(last10);
            if (!matches.isEmpty()) {
                // Prioritize OWNER or MANAGER if present
                userOpt = matches.stream()
                        .filter(u -> u.getRole() == UserRole.OWNER || u.getRole() == UserRole.MANAGER)
                        .findFirst()
                        .or(() -> Optional.of(matches.get(0)));
            }
        }

        if (userOpt.isPresent()) {
            User existing = userOpt.get();
            log.info("Matched user {} (Role: {}, Phone: {}) for phone input {}", existing.getName(), existing.getRole(), existing.getPhone(), phone);
            return existing;
        }

        // 3. Auto-register new customer
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
    }
}
