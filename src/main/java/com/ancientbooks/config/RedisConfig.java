package com.ancientbooks.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 * 使用 Spring Data Redis 4.x 推荐的序列化方式
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 配置 ObjectMapper
        ObjectMapper om = new ObjectMapper();
        om.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        // ✅ Spring Data Redis 4.x 推荐：自定义 RedisSerializer
        // 避免使用已废弃的 Jackson2JsonRedisSerializer 和 GenericJackson2JsonRedisSerializer

        // 创建 String 序列化器（用于 key）
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 创建 JSON 序列化器（用于 value）
        RedisSerializer<Object> jsonSerializer = new RedisSerializer<Object>() {
            @Override
            public byte[] serialize(Object object) {
                if (object == null) {
                    return new byte[0];
                }
                try {
                    return om.writeValueAsBytes(object);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize object", e);
                }
            }

            @Override
            public Object deserialize(byte[] bytes) {
                if (bytes == null || bytes.length == 0) {
                    return null;
                }
                try {
                    return om.readValue(bytes, Object.class);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to deserialize bytes", e);
                }
            }
        };

        // 设置序列化器
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
