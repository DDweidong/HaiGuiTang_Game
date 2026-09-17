package com.weidong.gamebackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weidong.gamebackend.common.PageResult;
import com.weidong.gamebackend.mapper.TurtleSoupMapper;
import com.weidong.gamebackend.model.TurtleSoup;
import com.weidong.gamebackend.service.TurtleSoupService;
import com.weidong.gamebackend.vo.TurtleSoupVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class TurtleSoupServiceImpl extends ServiceImpl<TurtleSoupMapper, TurtleSoup> implements TurtleSoupService {

    @Override
    public PageResult<TurtleSoupVO> pageSoups(long pageNum, long pageSize) {
        Page<TurtleSoup> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<TurtleSoup>().orderByDesc(TurtleSoup::getCompletedAt));
        return toPageResult(page);
    }

    @Override
    public PageResult<TurtleSoupVO> pageSoupsByUser(String userId, long pageNum, long pageSize) {
        Page<TurtleSoup> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<TurtleSoup>()
                        .eq(TurtleSoup::getUserId, userId)
                        .orderByDesc(TurtleSoup::getCompletedAt));
        return toPageResult(page);
    }

    @Override
    public TurtleSoupVO saveTurtleSoup(TurtleSoup soup) {
        save(soup);
        TurtleSoupVO vo = new TurtleSoupVO();
        BeanUtils.copyProperties(soup, vo);
        return vo;
    }

    /** 实体分页转 VO 分页，避免把 userId/roomId 暴露给前端 */
    private PageResult<TurtleSoupVO> toPageResult(Page<TurtleSoup> page) {
        return PageResult.of(
                page.getRecords().stream().map(this::toVo).toList(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize());
    }

    private TurtleSoupVO toVo(TurtleSoup soup) {
        TurtleSoupVO vo = new TurtleSoupVO();
        BeanUtils.copyProperties(soup, vo);
        return vo;
    }
}
