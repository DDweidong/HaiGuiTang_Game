package com.weidong.gamebackend.controller;

import com.weidong.gamebackend.common.ErrorCode;
import com.weidong.gamebackend.common.PageResult;
import com.weidong.gamebackend.common.Result;
import com.weidong.gamebackend.common.exception.BusinessException;
import com.weidong.gamebackend.dto.TurtleSoupCreateRequest;
import com.weidong.gamebackend.model.TurtleSoup;
import com.weidong.gamebackend.service.TurtleSoupService;
import com.weidong.gamebackend.vo.TurtleSoupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/turtle-soups")
@Tag(name = "海龟汤记录", description = "已完成游戏的查询与保存")
public class TurtleSoupController {

    @Autowired
    private TurtleSoupService turtleSoupService;

    @GetMapping
    @Operation(summary = "分页查询全部完成记录", description = "按完成时间倒序")
    public Result<PageResult<TurtleSoupVO>> pageAll(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(turtleSoupService.pageSoups(pageNum, pageSize));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "分页查询指定用户的完成记录", description = "按完成时间倒序")
    public Result<PageResult<TurtleSoupVO>> pageByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "userId 不能为空");
        }
        return Result.success(turtleSoupService.pageSoupsByUser(userId, pageNum, pageSize));
    }

    @PostMapping
    @Operation(summary = "保存一条完成记录", description = "id 与完成时间由服务端生成")
    public Result<TurtleSoupVO> create(@Valid @RequestBody TurtleSoupCreateRequest request) {
        TurtleSoup soup = new TurtleSoup();
        soup.setUserId(request.getUserId());
        soup.setRoomId(request.getRoomId());
        soup.setTitle(request.getTitle());
        soup.setSolution(request.getSolution());
        return Result.success(turtleSoupService.saveTurtleSoup(soup));
    }
}
