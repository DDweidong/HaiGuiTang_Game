package com.weidong.gamebackend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS 配置（Spring Security 层，由 SecurityConfig 的 http.cors() 引用）。
 * 必须在 Security 过滤链注册 CORS: 401/403 由 Security 过滤器直接写出响应、不经过 MVC 层，
 * 若只在 WebMvcConfigurer 配 CORS，这些错误响应会缺少 Access-Control-Allow-Origin 头，
 * 浏览器报 CORS 错误且前端拿不到 401 状态码，无法触发 token 自动刷新。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:5173")); // 允许的前端源
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // 允许携带认证头
        config.setMaxAge(3600L); // 预检请求缓存时间（秒）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
