package com.weidong.gamebackend.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.weidong.gamebackend.mapper.SoupQuestionMapper;
import com.weidong.gamebackend.mapper.TurtleSoupMapper;
import com.weidong.gamebackend.model.SoupQuestion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoupQuestionServiceTest {

    @Mock
    private SoupQuestionMapper soupQuestionMapper;
    @Mock
    private TurtleSoupMapper turtleSoupMapper;

    @InjectMocks
    private SoupQuestionService service;

    @Test
    void 新玩家优先简单题_简单题存在时不再随机() {
        when(turtleSoupMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        SoupQuestion simple = question("简单");
        when(soupQuestionMapper.pickByDifficulty("1", "简单")).thenReturn(simple);

        assertThat(service.pick(1L)).isSameAs(simple);
        verify(soupQuestionMapper, never()).pickAny(any());
    }

    @Test
    void 新玩家简单题抽空_回退随机() {
        when(turtleSoupMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(soupQuestionMapper.pickByDifficulty("1", "简单")).thenReturn(null);
        SoupQuestion anyQuestion = question("中等");
        when(soupQuestionMapper.pickAny("1")).thenReturn(anyQuestion);

        assertThat(service.pick(1L)).isSameAs(anyQuestion);
    }

    @Test
    void 老玩家_直接随机不再优先简单() {
        when(turtleSoupMapper.selectCount(any(Wrapper.class))).thenReturn(5L);
        SoupQuestion anyQuestion = question("困难");
        when(soupQuestionMapper.pickAny("1")).thenReturn(anyQuestion);

        assertThat(service.pick(1L)).isSameAs(anyQuestion);
        verify(soupQuestionMapper, never()).pickByDifficulty(any(), any());
    }

    @Test
    void 玩遍题库_返回null由调用方回退LLM即兴() {
        when(turtleSoupMapper.selectCount(any(Wrapper.class))).thenReturn(10L);
        when(soupQuestionMapper.pickAny("1")).thenReturn(null);

        assertThat(service.pick(1L)).isNull();
    }

    private SoupQuestion question(String difficulty) {
        SoupQuestion question = new SoupQuestion();
        question.setDifficulty(difficulty);
        return question;
    }
}
