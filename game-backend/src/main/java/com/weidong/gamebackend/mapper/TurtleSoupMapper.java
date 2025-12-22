package com.weidong.gamebackend.mapper;

import com.weidong.gamebackend.model.TurtleSoup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TurtleSoupMapper {

    // 查询所有海龟汤记录
    List<TurtleSoup> findAll();

    // 根据 user_id 查询该用户的所有海龟汤记录
    List<TurtleSoup> findByUserId(@Param("userId") String userId);

    // 新增一条海龟汤记录
    int insert(TurtleSoup turtleSoup);
}