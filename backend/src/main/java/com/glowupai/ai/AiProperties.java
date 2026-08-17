package com.glowupai.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * GlowUp AI 模型配置。
 */
@Component
@ConfigurationProperties(prefix = "glowup.ai")
public class AiProperties {

    /**
     * AI 服务提供方。
     */
    private String provider = "mock";

    /**
     * OpenAI API Key。
     */
    private String openaiApiKey = "";

    /**
     * OpenAI Responses API 地址。
     */
    private String openaiBaseUrl = "https://api.openai.com/v1/responses";

    /**
     * OpenAI 模型名。
     */
    private String openaiModel = "gpt-4.1-mini";

    /**
     * 请求超时时间，单位秒。
     */
    private int timeoutSeconds = 45;

    /**
     * 获取 AI 服务提供方。
     *
     * @return AI 服务提供方
     */
    public String getProvider() {
        return provider;
    }

    /**
     * 设置 AI 服务提供方。
     *
     * @param provider AI 服务提供方
     */
    public void setProvider(String provider) {
        this.provider = provider;
    }

    /**
     * 获取 OpenAI API Key。
     *
     * @return OpenAI API Key
     */
    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    /**
     * 设置 OpenAI API Key。
     *
     * @param openaiApiKey OpenAI API Key
     */
    public void setOpenaiApiKey(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
    }

    /**
     * 获取 OpenAI API 地址。
     *
     * @return OpenAI API 地址
     */
    public String getOpenaiBaseUrl() {
        return openaiBaseUrl;
    }

    /**
     * 设置 OpenAI API 地址。
     *
     * @param openaiBaseUrl OpenAI API 地址
     */
    public void setOpenaiBaseUrl(String openaiBaseUrl) {
        this.openaiBaseUrl = openaiBaseUrl;
    }

    /**
     * 获取 OpenAI 模型名。
     *
     * @return OpenAI 模型名
     */
    public String getOpenaiModel() {
        return openaiModel;
    }

    /**
     * 设置 OpenAI 模型名。
     *
     * @param openaiModel OpenAI 模型名
     */
    public void setOpenaiModel(String openaiModel) {
        this.openaiModel = openaiModel;
    }

    /**
     * 获取请求超时时间。
     *
     * @return 请求超时时间，单位秒
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * 设置请求超时时间。
     *
     * @param timeoutSeconds 请求超时时间，单位秒
     */
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 判断是否启用 OpenAI。
     *
     * @return 是否启用 OpenAI
     */
    public boolean openaiEnabled() {
        return AiProvider.fromValue(provider) == AiProvider.OPENAI;
    }
}
