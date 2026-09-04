package com.ancientbooks.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "coze")
public class CozeProperties {
    private String baseUrl;
    private String patToken;
    private String botId;
}
