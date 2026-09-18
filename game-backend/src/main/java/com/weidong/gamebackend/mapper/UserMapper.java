package com.weidong.gamebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidong.gamebackend.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
