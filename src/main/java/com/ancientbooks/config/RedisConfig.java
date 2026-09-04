package com.ancientbooks.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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

        // 使用构造函数传入 ObjectMapper（避免使用已过时的 setObjectMapper）
        Jackson2JsonRedisSerializer<Object> jackson = new Jackson2JsonRedisSerializer<>(om, Object.class);

        StringRedisSerializer string = new StringRedisSerializer();
        template.setKeySerializer(string);
        template.setHashKeySerializer(string);
        template.setValueSerializer(jackson);
        template.setHashValueSerializer(jackson);
        template.afterPropertiesSet();
        return template;
    }
}
