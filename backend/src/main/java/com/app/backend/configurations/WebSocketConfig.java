package com.app.backend.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://103.200.21.203}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> originPatterns = new ArrayList<>();

        // 2. Luôn cho phép localhost với mọi port (Pattern gốc)
        originPatterns.add("http://localhost:*");

        // 3. Xử lý danh sách từ config
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            String[] origins = allowedOrigins.split(",");
            for (String origin : origins) {
                String trimmedOrigin = origin.trim();
                if (!trimmedOrigin.isEmpty()) {
                    // Thêm chính xác origin đó
                    originPatterns.add(trimmedOrigin);
                    // Thêm pattern cho phép dynamic port (như logic cũ của bạn)
                    originPatterns.add(trimmedOrigin + ":*");
                }
            }
        }

        // 4. Đăng ký Endpoint với danh sách patterns đã gộp
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(originPatterns.toArray(new String[0]))
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Client gửi tin → /app/**
        registry.setApplicationDestinationPrefixes("/app");

        // Server gửi broadcast → /topic/**
        registry.enableSimpleBroker("/topic", "/topic/notifications", "/topic/chat/**");

        // Nếu gửi riêng user
        registry.setUserDestinationPrefix("/user");
    }
}
