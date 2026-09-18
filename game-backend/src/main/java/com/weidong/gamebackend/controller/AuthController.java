package com.weidong.gamebackend.controller;

import com.weidong.gamebackend.common.ErrorCode;
import com.weidong.gamebackend.common.Result;
import com.weidong.gamebackend.common.exception.BusinessException;
import com.weidong.gamebackend.dto.LoginRequest;
import com.weidong.gamebackend.dto.RefreshRequest;
import com.weidong.gamebackend.dto.RegisterRequest;
import com.weidong.gamebackend.model.User;
import com.weidong.gamebackend.security.JwtUtil;
import com.weidong.gamebackend.security.LoginUser;
import com.weidong.gamebackend.service.UserService;
import com.weidong.gamebackend.vo.TokenVO;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "用户认证", description = "注册、登录与令牌刷新")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "注册", description = "注册成功后直接返回令牌对（注册即登录）")
    public Result<TokenVO> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.getUsername(), request.getPassword());
        return Result.success(issueTokens(user));
    }

    @PostMapping("/login")
    @Operation(summary = "登录", description = "校验用户名密码，返回 access/refresh 令牌对")
    public Result<TokenVO> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        return Result.success(issueTokens(user));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新登录态", description = "用 refreshToken 换取新 accessToken；refresh 过期需重新登录")
    public Result<TokenVO> refresh(@Valid @RequestBody RefreshRequest request) {
        LoginUser user;
        try {
            user = jwtUtil.parseRefreshToken(request.getRefreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // refresh 时校验用户仍存在，防止已注销用户继续换发令牌
        if (userService.getById(user.id()) == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        // 设计决策: 暂不做 refresh 轮换 —— 无状态 JWT 无法作废旧 token，
        // 轮换需要 Redis 黑名单/版本号存储，待 W3 引入 Redis 后升级
        String accessToken = jwtUtil.generateAccessToken(user.id(), user.username());
        return Result.success(new TokenVO(accessToken, request.getRefreshToken(), user.id(), user.username()));
    }

    private TokenVO issueTokens(User user) {
        return new TokenVO(
                jwtUtil.generateAccessToken(user.getId(), user.getUsername()),
                jwtUtil.generateRefreshToken(user.getId(), user.getUsername()),
                user.getId(),
                user.getUsername());
    }
}
