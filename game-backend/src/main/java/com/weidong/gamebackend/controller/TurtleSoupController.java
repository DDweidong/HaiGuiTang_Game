package com.weidong.gamebackend.controller;

import com.weidong.gamebackend.common.PageResult;
import com.weidong.gamebackend.common.Result;
import com.weidong.gamebackend.dto.TurtleSoupCreateRequest;
import com.weidong.gamebackend.model.TurtleSoup;
import com.weidong.gamebackend.security.LoginUser;
import com.weidong.gamebackend.security.SecurityUtil;
import com.weidong.gamebackend.service.TurtleSoupService;
import com.weidong.gamebackend.vo.TurtleSoupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/turtle-soups")
@Tag(name = "海龟汤记录", description = "已完成游戏的查询与保存")
public class TurtleSoupController {

    @Autowired
    private TurtleSoupService turtleSoupService;

    @GetMapping
    @Operation(summary = "分页查询当前用户的完成记录", description = "按完成时间倒序；用户从登录态获取，只能查本人")
    public Result<PageResult<TurtleSoupVO>> pageMine(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        LoginUser loginUser = SecurityUtil.currentUser();
        return Result.success(turtleSoupService.pageSoupsByUser(String.valueOf(loginUser.id()), pageNum, pageSize));
    }

    @PostMapping
    @Operation(summary = "保存一条完成记录", description = "id 与完成时间由服务端生成；用户从登录态获取，不接受客户端传入")
    public Result<TurtleSoupVO> create(@Valid @RequestBody TurtleSoupCreateRequest request) {
        LoginUser loginUser = SecurityUtil.currentUser();
        TurtleSoup soup = new TurtleSoup();
        soup.setUserId(String.valueOf(loginUser.id()));
        soup.setRoomId(request.getRoomId());
        soup.setTitle(request.getTitle());
        soup.setSolution(request.getSolution());
        return Result.success(turtleSoupService.saveTurtleSoup(soup));
    }
}
