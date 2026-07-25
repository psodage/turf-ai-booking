package com.turfai.booking.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filter that enforces rate limiting per client IP address.
 * Webhook rate limit: 100 req/min (ADR-015).
 * Public API rate limit: 300 req/min.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {

    public static final int WEBHOOK_RATE_LIMIT = 100;
    public static final int GLOBAL_API_RATE_LIMIT = 300;

    private final Map<String, RateBucket> webhookBuckets = new ConcurrentHashMap<>();
    private final Map<String, RateBucket> apiBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        if (path.contains("/webhook/whatsapp") || path.contains("/webhooks/whatsapp")) {
            if (!allowRequest(webhookBuckets, clientIp, WEBHOOK_RATE_LIMIT)) {
                log.warn("Rate limit exceeded for WhatsApp webhook endpoint from IP: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too Many Requests");
                return;
            }
        } else if (path.startsWith("/api/v1/")) {
            if (!allowRequest(apiBuckets, clientIp, GLOBAL_API_RATE_LIMIT)) {
                log.warn("Rate limit exceeded for public API from IP: {}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Too Many Requests");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean allowRequest(Map<String, RateBucket> bucketMap, String key, int maxRequestsPerMinute) {
        long currentMinute = System.currentTimeMillis() / 60000;
        RateBucket bucket = bucketMap.computeIfAbsent(key, k -> new RateBucket(currentMinute));

        synchronized (bucket) {
            if (bucket.minuteWindow != currentMinute) {
                bucket.minuteWindow = currentMinute;
                bucket.count.set(0);
            }

            if (bucket.count.incrementAndGet() > maxRequestsPerMinute) {
                return false;
            }
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateBucket {
        long minuteWindow;
        final AtomicInteger count = new AtomicInteger(0);

        RateBucket(long minuteWindow) {
            this.minuteWindow = minuteWindow;
        }
    }
}
