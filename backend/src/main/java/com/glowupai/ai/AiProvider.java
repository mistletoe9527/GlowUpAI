package com.glowupai.ai;

import java.util.Arrays;

/**
 * AI 服务提供方枚举。
 */
public enum AiProvider {
    /**
     * 使用本地规则模拟。
     */
    MOCK,

    /**
     * 使用 OpenAI Responses API。
     */
    OPENAI;

    /**
     * 按配置字符串解析 provider。
     *
     * @param value 配置值
     * @return AI provider
     */
    public static AiProvider fromValue(String value) {
        if (value == null || value.isBlank()) {
            return MOCK;
        }
        return Arrays.stream(values())
                .filter(provider -> provider.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElse(MOCK);
    }
}
