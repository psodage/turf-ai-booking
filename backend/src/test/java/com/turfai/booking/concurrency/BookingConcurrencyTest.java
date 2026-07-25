package com.turfai.booking.concurrency;

import com.turfai.booking.dto.request.CreateBookingHoldRequest;
import com.turfai.booking.entity.Business;
import com.turfai.booking.entity.BusinessStatus;
import com.turfai.booking.entity.OperatingHours;
import com.turfai.booking.entity.PricingRule;
import com.turfai.booking.entity.PricingType;
import com.turfai.booking.entity.Turf;
import com.turfai.booking.entity.TurfStatus;
import com.turfai.booking.entity.TurfType;
import com.turfai.booking.entity.User;
import com.turfai.booking.entity.UserRole;
import com.turfai.booking.entity.UserStatus;
import com.turfai.booking.exception.SlotUnavailableException;
import com.turfai.booking.repository.BusinessRepository;
import com.turfai.booking.repository.OperatingHoursRepository;
import com.turfai.booking.repository.PricingRuleRepository;
import com.turfai.booking.repository.TurfRepository;
import com.turfai.booking.repository.UserRepository;
import com.turfai.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BookingConcurrencyTest {

    @Autowired private BookingService bookingService;
    @Autowired private BusinessRepository businessRepository;
    @Autowired private TurfRepository turfRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OperatingHoursRepository operatingHoursRepository;
    @Autowired private PricingRuleRepository pricingRuleRepository;

    private Turf testTurf;
    private User testCustomer1;
    private User testCustomer2;
    private LocalDate targetDate;

    @BeforeEach
    void setUp() {
        Business business = businessRepository.save(Business.builder()
                .name("Concurrency Arena")
                .whatsappPhoneNumberId("PN_CONCURRENCY_" + System.currentTimeMillis())
                .timezone("Asia/Kolkata")
                .status(BusinessStatus.ACTIVE)
                .build());

        testTurf = turfRepository.save(Turf.builder()
                .business(business)
                .name("Turf A")
                .type(TurfType.FIVE_A_SIDE)
                .status(TurfStatus.ACTIVE)
                .build());

        testCustomer1 = userRepository.save(User.builder()
                .name("Customer One")
                .phone("+919000000001_" + System.currentTimeMillis())
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        testCustomer2 = userRepository.save(User.builder()
                .name("Customer Two")
                .phone("+919000000002_" + System.currentTimeMillis())
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        targetDate = LocalDate.now().plusDays(5);

        // Operating hours: Mon-Sun 06:00 to 23:00 for targetDate day_of_week
        int dayOfWeekIndex = targetDate.getDayOfWeek().getValue() - 1;
        operatingHoursRepository.save(OperatingHours.builder()
                .turf(testTurf)
                .dayOfWeek(dayOfWeekIndex)
                .openingTime(LocalTime.of(6, 0))
                .closingTime(LocalTime.of(23, 0))
                .isClosed(false)
                .build());

        pricingRuleRepository.save(PricingRule.builder()
                .turf(testTurf)
                .pricingType(PricingType.BASE)
                .amount(new BigDecimal("1000.00"))
                .build());
    }

    @Test
    @DisplayName("Concurrent booking hold requests for the exact same slot must prevent double-booking")
    void testConcurrentBookingHoldsPreventDoubleBooking() throws InterruptedException {
        LocalTime startTime = LocalTime.of(18, 0);
        LocalTime endTime = LocalTime.of(19, 0);

        CreateBookingHoldRequest req1 = CreateBookingHoldRequest.builder()
                .turfId(testTurf.getId())
                .customerId(testCustomer1.getId())
                .bookingDate(targetDate)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        CreateBookingHoldRequest req2 = CreateBookingHoldRequest.builder()
                .turfId(testTurf.getId())
                .customerId(testCustomer2.getId())
                .bookingDate(targetDate)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Callable<Boolean>> tasks = List.of(
                () -> {
                    try {
                        bookingService.createBookingHold(req1);
                        return true;
                    } catch (Exception ex) {
                        return false;
                    }
                },
                () -> {
                    try {
                        bookingService.createBookingHold(req2);
                        return true;
                    } catch (Exception ex) {
                        return false;
                    }
                }
        );

        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        int successCount = 0;
        int failureCount = 0;

        for (Future<Boolean> future : futures) {
            try {
                if (future.get()) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (ExecutionException e) {
                failureCount++;
            }
        }

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);
    }
}
