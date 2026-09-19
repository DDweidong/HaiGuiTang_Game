package com.weidong.gamebackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameStateServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private GameStateService service;

    @Test
    void 键不存在视为RUNNING_新会话无需初始化() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("hgt:game-state:m1")).thenReturn(null);

        assertThat(service.get("m1")).isEqualTo(GameStateService.GameState.RUNNING);
        assertThat(service.isTerminal("m1")).isFalse();
    }

    @Test
    void 终态判定_SOLVED与REVEALED均视为结束() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("hgt:game-state:m1")).thenReturn("REVEALED");

        assertThat(service.get("m1")).isEqualTo(GameStateService.GameState.REVEALED);
        assertThat(service.isTerminal("m1")).isTrue();
    }

    @Test
    void markSolved与markRevealed_写入终态且带TTL() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        service.markSolved("m1");
        service.markRevealed("m1");

        verify(valueOps).set("hgt:game-state:m1", "SOLVED", Duration.ofDays(7));
        verify(valueOps).set("hgt:game-state:m1", "REVEALED", Duration.ofDays(7));
    }

    @Test
    void 题库题目ID存取() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("hgt:game-question:m1")).thenReturn("12");

        assertThat(service.getQuestion("m1")).isEqualTo(12L);

        service.setQuestion("m1", 12L);
        verify(valueOps).set("hgt:game-question:m1", "12", Duration.ofDays(7));
    }

    @Test
    void 即兴局无题目记录_getQuestion返回null() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("hgt:game-question:m1")).thenReturn(null);

        assertThat(service.getQuestion("m1")).isNull();
    }
}
