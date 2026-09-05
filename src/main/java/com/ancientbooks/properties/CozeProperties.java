package com.ancientbooks.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "coze")
public class CozeProperties {
    /**
     * V2 API 地址（保留兼容）
     */
    private String baseUrlV2;

    /**
     * V3 API 地址
     */
    private String baseUrlV3;

    /**
     * 当前使用的API版本：v3 或 v2
     */
    private String apiVersion;

    /**
     * PAT Token
     */
    private String patToken;

    /**
     * Bot ID
     */
    private String botId;

    /**
     * 工作流ID（用于工作流调用）
     */
    private String workflowId;

    /**
     * 超时时间（毫秒）
     */
    private Integer timeout;

    /**
     * 获取当前使用的API地址
     */
    public String getBaseUrl() {
        return "v3".equalsIgnoreCase(apiVersion) ? baseUrlV3 : baseUrlV2;
    }
}
