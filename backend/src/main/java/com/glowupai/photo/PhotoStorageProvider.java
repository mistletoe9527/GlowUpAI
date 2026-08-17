package com.glowupai.photo;

import java.util.Arrays;

/**
 * 照片对象存储提供方。
 */
public enum PhotoStorageProvider {
    /**
     * 本地文件存储。
     */
    LOCAL,

    /**
     * AWS S3 对象存储。
     */
    S3;

    /**
     * 按配置解析存储提供方。
     *
     * @param value 配置值
     * @return 存储提供方
     */
    public static PhotoStorageProvider fromValue(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL;
        }
        return Arrays.stream(values())
                .filter(provider -> provider.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported photo storage provider: " + value));
    }
}
