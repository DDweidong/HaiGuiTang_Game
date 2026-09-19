package com.weidong.gamebackend.controller;

import com.weidong.gamebackend.common.PageResult;
import com.weidong.gamebackend.common.Result;
import com.weidong.gamebackend.security.LoginUser;
import com.weidong.gamebackend.security.SecurityUtil;
import com.weidong.gamebackend.service.TokenUsageService;
import com.weidong.gamebackend.vo.TokenUsageSummaryVO;
import com.weidong.gamebackend.vo.TokenUsageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/token-usage")
@Tag(name = "用量统计", description = "LLM token 用量与成本查询")
public class TokenUsageController {

    @Autowired
    private TokenUsageService tokenUsageService;

    @GetMapping("/summary")
    @Operation(summary = "当前用户的累计用量与估算费用", description = "用户从登录态获取，只能查本人")
    public Result<TokenUsageSummaryVO> summary() {
        LoginUser loginUser = SecurityUtil.currentUser();
        return Result.success(tokenUsageService.summary(loginUser.id()));
    }

    @GetMapping("/records")
    @Operation(summary = "分页查询当前用户的调用明细", description = "按调用时间倒序；用户从登录态获取，只能查本人")
    public Result<PageResult<TokenUsageVO>> records(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        LoginUser loginUser = SecurityUtil.currentUser();
        return Result.success(tokenUsageService.pageByUser(loginUser.id(), pageNum, pageSize));
    }
}
