package com.app.backend.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);
    @Value("${spring.data.redis.host}") // Read 'spring.data.redis.host' property from application.yml
    private String redisHost;

    @Value("${spring.data.redis.port}") // Read 'spring.data.redis.port' property from application.yml
    private int redisPort;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        logger.info(String.format("redisHost = %s, redisPort = %d", redisHost, redisPort));
        RedisStandaloneConfiguration configuration =
                new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(configuration);
    }
    @Bean
    public RedisTemplate<String, Object> redisTemplate()

    {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Đăng ký JavaTimeModule để hỗ trợ tất cả Java 8 date/time types
        // Bao gồm: Instant, LocalDateTime, LocalDate, ZonedDateTime, etc.
        objectMapper.registerModule(new JavaTimeModule());
        
        // Serialize dates as ISO-8601 strings thay vì timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return objectMapper;
    }
}