package com.urlshortener.filter;

import com.urlshortener.cache.RedisService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * =============================================================
 * RATE LIMIT FILTER
 * =============================================================
 *
 * WHAT IS A FILTER?
 *   A Servlet Filter intercepts EVERY HTTP request BEFORE it
 *   reaches any controller. It's middleware — sits between the
 *   HTTP server and your application code.
 *
 *   Request flow with filters:
 *     HTTP Request
 *         ↓
 *     [Filter 1] → [Filter 2] → [RateLimitFilter]
 *         ↓
 *     DispatcherServlet
 *         ↓
 *     Controller
 *         ↓
 *     Service
 *
 * OncePerRequestFilter:
 *   Spring base class that guarantees the filter runs exactly ONCE
 *   per request (prevents double-execution in some redirect scenarios).
 *   Just extend it and implement doFilterInternal().
 *
 * @Component:
 *   Spring auto-detects this as a bean and registers it as a filter.
 *   No extra configuration needed!
 *
 * =============================================================
 * RATE LIMITING STRATEGY: Token Bucket via Redis
 * =============================================================
 *
 * Token Bucket concept:
 *   - Each IP gets a "bucket" with N tokens (= N requests allowed)
 *   - Each request consumes 1 token
 *   - Bucket refills after the time window (redis key expires)
 *
 * In our implementation:
 *   - Redis key: "ratelimit:1.2.3.4"
 *   - Value: request count (integer, incremented per request)
 *   - TTL: 60 seconds (window)
 *
 * Example:
 *   Request #1  from 1.2.3.4 → count=1  → allowed, TTL set to 60s
 *   Request #50 from 1.2.3.4 → count=50 → allowed
 *   Request #100 from 1.2.3.4 → count=100 → allowed (at limit)
 *   Request #101 from 1.2.3.4 → count=101 → BLOCKED → HTTP 429
 *   [After 60s, key expires, count resets]
 *   Request #1  from 1.2.3.4 → count=1  → allowed again
 *
 * =============================================================
 * WHY RATE LIMIT?
 * =============================================================
 *   1. SPAM PROTECTION: Prevent bots from creating millions of short URLs
 *   2. DDOS MITIGATION: Slow down flood attacks
 *   3. FAIR USE: Ensure one client can't monopolize server resources
 *   4. COST CONTROL: DB writes and Redis ops cost money at scale
 * =============================================================
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisService redisService;

    @Value("${app.rate-limit.max-requests:100}")
    private int maxRequests;

    @Value("${app.rate-limit.window-minutes:1}")
    private int windowMinutes;

    /**
     * Core filter logic — runs once per HTTP request.
     *
     * @param request    The incoming HTTP request
     * @param response   The HTTP response (can be modified to short-circuit)
     * @param filterChain Passes control to the next filter / controller
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Get the client's IP address
        // X-Forwarded-For header exists when behind a proxy/load balancer
        String clientIp = getClientIp(request);

        log.debug("Request from IP: {} | URI: {}", clientIp, request.getRequestURI());

        // Check rate limit in Redis
        boolean allowed = redisService.isRateLimitAllowed(clientIp, maxRequests, windowMinutes);

        if (!allowed) {
            // Short-circuit: don't call the controller, return 429 immediately
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE); // 429
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\": \"Rate limit exceeded. Max " + maxRequests +
                " requests per " + windowMinutes + " minute(s). Please slow down.\"}"
            );
            return;
            // By returning without calling filterChain.doFilter(), we
            // prevent the request from reaching the controller.
        }

        // Rate limit OK — pass the request to the next filter/controller
        filterChain.doFilter(request, response);
    }

    /**
     * Extract client IP address from the request.
     *
     * WHY CHECK X-Forwarded-For?
     *   When your app is behind a load balancer or reverse proxy (Nginx),
     *   request.getRemoteAddr() returns the PROXY's IP, not the client's.
     *   The real client IP is in the X-Forwarded-For header.
     *
     *   X-Forwarded-For: clientIP, proxy1IP, proxy2IP
     *   We want the FIRST entry (the original client).
     *
     * @param request   The HTTP request
     * @return          The real client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Could be comma-separated: "clientIP, proxy1, proxy2"
            // We want the original client = first one
            return xForwardedFor.split(",")[0].trim();
        }

        // No proxy — get direct remote address
        return request.getRemoteAddr();
    }

    /**
     * Skip rate limiting for health check and actuator endpoints.
     *
     * shouldNotFilter() is called by OncePerRequestFilter before
     * doFilterInternal(). Return true to SKIP the filter for that request.
     *
     * WHY SKIP /actuator?
     *   Health monitoring probes (from load balancers, Docker) hit
     *   /actuator/health very frequently. We don't want them to trigger
     *   rate limits.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }
}
