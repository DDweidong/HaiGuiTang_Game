package com.weidong.gamebackend.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档元信息，在线地址 /swagger-ui.html。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("海龟汤 AI 游戏接口文档")
                .description("AI 主持人对话与游戏完成记录相关接口")
                .version("v1.0.0"));
    }
}
