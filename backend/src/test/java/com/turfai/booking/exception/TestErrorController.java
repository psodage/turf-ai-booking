package com.turfai.booking.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller strictly for testing GlobalExceptionHandler response mapping.
 */
@RestController
@RequestMapping("/api/v1/test-errors")
public class TestErrorController {

    public record TestRequest(@NotNull(message = "name must not be null") String name) {}

    @GetMapping("/slot-unavailable")
    public void throwSlotUnavailable() {
        throw new SlotUnavailableException("Slot is already booked.", List.of("18:00", "20:00"));
    }

    @PostMapping("/validation")
    public void triggerValidation(@Valid @RequestBody TestRequest request) {
    }
}
