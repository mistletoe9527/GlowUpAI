package com.glowupai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GlowUp AI 后端启动入口。
 */
@SpringBootApplication
public class GlowUpAiBackendApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GlowUpAiBackendApplication.class, args);
    }
}
