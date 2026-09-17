package com.weidong.gamebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidong.gamebackend.model.TurtleSoup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 完成记录 Mapper。
 * 继承 BaseMapper 获得单表通用 CRUD；复杂 SQL 再补充自定义方法。
 */
@Mapper
public interface TurtleSoupMapper extends BaseMapper<TurtleSoup> {
}
