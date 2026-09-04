package com.ancientbooks.constants;

/**
 * 系统常量
 */
public class SystemConstants {

    private SystemConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /**
     * 默认用户ID
     */
    public static final String DEFAULT_USER_ID = "anonymous";

    /**
     * 会话缓存前缀
     */
    public static final String CHAT_CONV_PREFIX = "chat:conv:";

    /**
     * 会话过期时间（分钟）
     */
    public static final int CONV_EXPIRE_MINUTES = 30;

    /**
     * SSE 超时时间（毫秒）
     */
    public static final long SSE_TIMEOUT_MS = 180_000L;

    /**
     * 单次输入最大长度
     */
    public static final int MAX_QUERY_LENGTH = 5000;
}
