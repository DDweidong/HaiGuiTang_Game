package com.weidong.gamebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.weidong.gamebackend.mapper.SoupQuestionMapper;
import com.weidong.gamebackend.mapper.TurtleSoupMapper;
import com.weidong.gamebackend.model.SoupQuestion;
import com.weidong.gamebackend.model.TurtleSoup;
import org.springframework.stereotype.Service;

/**
 * 题库抽题服务。
 * 策略: 新玩家（完成 0~2 局）优先抽简单题，降低入门门槛防劝退；
 * 简单题抽完或老玩家则完全随机（排除该用户已玩过的题）。
 * 返回 null 表示该用户玩遍题库（或题库为空），由调用方回退 LLM 即兴出题。
 */
@Service
public class SoupQuestionService {

    /** 新玩家界定: 完成局数小于该值时优先简单题 */
    private static final long NEW_PLAYER_GAMES = 3;

    private static final String DIFFICULTY_SIMPLE = "简单";

    private final SoupQuestionMapper soupQuestionMapper;
    private final TurtleSoupMapper turtleSoupMapper;

    public SoupQuestionService(SoupQuestionMapper soupQuestionMapper, TurtleSoupMapper turtleSoupMapper) {
        this.soupQuestionMapper = soupQuestionMapper;
        this.turtleSoupMapper = turtleSoupMapper;
    }

    /** 为该用户抽一道没玩过的题；null 表示无题可抽（回退 LLM 即兴） */
    public SoupQuestion pick(Long userId) {
        String userIdStr = String.valueOf(userId);
        long completed = turtleSoupMapper.selectCount(
                new LambdaQueryWrapper<TurtleSoup>().eq(TurtleSoup::getUserId, userIdStr));

        SoupQuestion question = null;
        if (completed < NEW_PLAYER_GAMES) {
            question = soupQuestionMapper.pickByDifficulty(userIdStr, DIFFICULTY_SIMPLE);
        }
        if (question == null) {
            question = soupQuestionMapper.pickAny(userIdStr);
        }
        return question;
    }

    public SoupQuestion getById(Long id) {
        return id == null ? null : soupQuestionMapper.selectById(id);
    }
}
