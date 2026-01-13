package com.app.backend.filters;

import com.app.backend.configurations.RateLimitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting Filter
 * 
 * Áp dụng rate limit dựa trên:
 * - IP address (cho anonymous users)
 * - User ID (cho authenticated users)
 * 
 * Thứ tự ưu tiên filter: HIGHEST (chạy đầu tiên)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    // Các endpoint không cần rate limit
    private static final Set<String> EXCLUDED_PATHS = Set.of(
        "/actuator/health",
        "/actuator/info",
        "/ws",           // WebSocket
        "/favicon.ico",
        "/error"
    );

    // Các endpoint Auth cần giới hạn nghiêm ngặt
    private static final Set<String> AUTH_PATHS = Set.of(
        "/api/v1/users/login",
        "/api/v1/users/register",
        "/api/v1/users/forgot-password",
        "/api/v1/users/reset-password"
    );

    // Các endpoint Upload cần giới hạn
    private static final Set<String> UPLOAD_PATHS = Set.of(
        "/api/v1/users/uploads",
        "/api/v1/cau-hoi/upload",
        "/api/v1/chat/upload"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Bỏ qua nếu rate limit bị tắt
        if (!rateLimitConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Bỏ qua các path không cần rate limit
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bỏ qua OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Lấy key định danh (user ID hoặc IP)
        String rateLimitKey = getRateLimitKey(request);
        
        // Chọn bucket phù hợp dựa trên endpoint
        Bucket bucket = selectBucket(path, rateLimitKey);
        
        // Thử consume 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Còn token → cho phép request
            // Thêm headers để client biết còn bao nhiêu requests
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.addHeader("X-Rate-Limit-Reset", 
                    String.valueOf(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())));
            
            filterChain.doFilter(request, response);
        } else {
            // Hết token → reject request
            long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            
            log.warn("⚠️ Rate limit exceeded for key={}, path={}, wait={}s", 
                    rateLimitKey, path, waitSeconds);
            
            sendRateLimitExceededResponse(response, waitSeconds);
        }
    }

    /**
     * Lấy key định danh cho rate limiting
     * Ưu tiên: User ID > IP Address
     */
    private String getRateLimitKey(HttpServletRequest request) {
        // Thử lấy user từ Security Context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }
        
        // Fallback: dùng IP address
        String ip = getClientIpAddress(request);
        return "ip:" + ip;
    }

    /**
     * Lấy IP thực của client (xử lý reverse proxy)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For có thể chứa nhiều IP, lấy cái đầu tiên
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Chọn bucket phù hợp dựa trên endpoint
     */
    private Bucket selectBucket(String path, String key) {
        if (isAuthPath(path)) {
            return rateLimitConfig.getAuthBucket(key);
        }
        if (isUploadPath(path)) {
            return rateLimitConfig.getUploadBucket(key);
        }
        return rateLimitConfig.getApiBucket(key);
    }

    /**
     * Kiểm tra path có được exclude không
     */
    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Kiểm tra có phải Auth endpoint không
     */
    private boolean isAuthPath(String path) {
        return AUTH_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Kiểm tra có phải Upload endpoint không
     */
    private boolean isUploadPath(String path) {
        return UPLOAD_PATHS.stream().anyMatch(path::contains);
    }

    /**
     * Gửi response khi bị rate limit
     */
    private void sendRateLimitExceededResponse(HttpServletResponse response, long waitSeconds) 
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.addHeader("Retry-After", String.valueOf(waitSeconds));
        
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("status", 429);
        errorBody.put("error", "Too Many Requests");
        errorBody.put("message", "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau " + waitSeconds + " giây.");
        errorBody.put("retryAfterSeconds", waitSeconds);
        
        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
