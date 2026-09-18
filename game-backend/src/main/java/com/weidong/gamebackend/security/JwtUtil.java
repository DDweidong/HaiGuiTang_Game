package com.weidong.gamebackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与校验（JJWT 0.12 API）。
 * 双 token 设计: access 30 分钟用于接口鉴权；refresh 7 天仅用于换取新 access，
 * 过期则需重新登录。密钥 HS256 签名，生产环境由 JWT_SECRET 环境变量注入。
 */
@Component
public class JwtUtil {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_USERNAME = "username";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.access-expiration}") long accessExpiration,
                   @Value("${jwt.refresh-expiration}") long refreshExpiration) {
        // HS256 要求密钥至少 256 bit（32 字节）
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(Long userId, String username) {
        return generate(userId, username, TYPE_ACCESS, accessExpiration);
    }

    public String generateRefreshToken(Long userId, String username) {
        return generate(userId, username, TYPE_REFRESH, refreshExpiration);
    }

    /** 解析 access token；过期/篡改/类型不匹配抛 JwtException，由调用方决定如何处理 */
    public LoginUser parseAccessToken(String token) {
        return parse(token, TYPE_ACCESS);
    }

    /** 解析 refresh token */
    public LoginUser parseRefreshToken(String token) {
        return parse(token, TYPE_REFRESH);
    }

    private String generate(Long userId, String username, String type, long expirationMillis) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMillis))
                .signWith(key)
                .compact();
    }

    private LoginUser parse(String token, String expectedType) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new JwtException("token 类型不匹配");
        }
        return new LoginUser(Long.valueOf(claims.getSubject()), claims.get(CLAIM_USERNAME, String.class));
    }
}
