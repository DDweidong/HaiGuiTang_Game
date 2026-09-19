package com.weidong.gamebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.weidong.gamebackend.model.SoupQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 题库 Mapper。
 * 抽取规则: 排除该用户已玩过的题（completed_games.question_id 关联），
 * 同一道题可给不同用户重复用——真相只对玩过的人剧透。
 * ORDER BY RAND() 在题库量级（几百条以内）下足够简单可靠。
 */
@Mapper
public interface SoupQuestionMapper extends BaseMapper<SoupQuestion> {

    /** 按难度抽一道该用户没玩过的题 */
    @Select("SELECT q.* FROM soup_questions q " +
            "WHERE q.difficulty = #{difficulty} " +
            "AND q.id NOT IN (SELECT question_id FROM completed_games " +
            "                WHERE user_id = #{userId} AND question_id IS NOT NULL) " +
            "ORDER BY RAND() LIMIT 1")
    SoupQuestion pickByDifficulty(@Param("userId") String userId, @Param("difficulty") String difficulty);

    /** 抽任意一道该用户没玩过的题 */
    @Select("SELECT q.* FROM soup_questions q " +
            "WHERE q.id NOT IN (SELECT question_id FROM completed_games " +
            "                WHERE user_id = #{userId} AND question_id IS NOT NULL) " +
            "ORDER BY RAND() LIMIT 1")
    SoupQuestion pickAny(@Param("userId") String userId);
}
