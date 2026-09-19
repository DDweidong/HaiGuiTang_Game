package com.weidong.gamebackend.tool;

import com.weidong.gamebackend.model.SoupQuestion;
import com.weidong.gamebackend.model.TurtleSoup;
import com.weidong.gamebackend.security.LoginUser;
import com.weidong.gamebackend.service.GameStateService;
import com.weidong.gamebackend.service.SoupQuestionService;
import com.weidong.gamebackend.service.TurtleSoupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameResultToolTest {

    @Mock
    private TurtleSoupService turtleSoupService;
    @Mock
    private GameStateService gameStateService;
    @Mock
    private SoupQuestionService soupQuestionService;

    @InjectMocks
    private GameResultTool tool;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 题库局_忽略LLM传参_用库内标题汤底落库() {
        when(gameStateService.getQuestion("5/100")).thenReturn(1L);
        SoupQuestion question = new SoupQuestion();
        question.setId(1L);
        question.setTitle("库内标题");
        question.setTangDi("库内真相");
        when(soupQuestionService.getById(1L)).thenReturn(question);

        String reply = tool.saveGameResult("5/100", "LLM 自由发挥的标题", "LLM 幻觉的真相");

        ArgumentCaptor<TurtleSoup> captor = ArgumentCaptor.forClass(TurtleSoup.class);
        verify(turtleSoupService).saveTurtleSoup(captor.capture());
        TurtleSoup record = captor.getValue();
        assertThat(record.getTitle()).isEqualTo("库内标题");
        assertThat(record.getSolution()).isEqualTo("库内真相");
        assertThat(record.getQuestionId()).isEqualTo(1L);
        // 无 SecurityContext 时回退解析 memoryId 前缀（"userId/timestamp" 格式）
        assertThat(record.getUserId()).isEqualTo("5");
        assertThat(record.getRoomId()).isEqualTo("5/100");
        assertThat(record.getCompletedAt()).isNotNull();
        verify(gameStateService).markSolved("5/100");
        // 返回值进入 LLM 上下文参与下一轮生成，必须引导结束格式而非"确认保存成功"
        assertThat(reply).contains("游戏结束");
    }

    @Test
    void 即兴局_使用LLM传入的标题汤底() {
        when(gameStateService.getQuestion("5/100")).thenReturn(null);
        when(soupQuestionService.getById(null)).thenReturn(null);

        tool.saveGameResult("5/100", "即兴标题", "即兴真相");

        ArgumentCaptor<TurtleSoup> captor = ArgumentCaptor.forClass(TurtleSoup.class);
        verify(turtleSoupService).saveTurtleSoup(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("即兴标题");
        assertThat(captor.getValue().getSolution()).isEqualTo("即兴真相");
        assertThat(captor.getValue().getQuestionId()).isNull();
        verify(gameStateService).markSolved("5/100");
    }

    @Test
    void SecurityContext存在时_用户ID从登录态取_不解析memoryId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser(99L, "tester"), null));

        tool.saveGameResult("5/100", "标题", "真相");

        ArgumentCaptor<TurtleSoup> captor = ArgumentCaptor.forClass(TurtleSoup.class);
        verify(turtleSoupService).saveTurtleSoup(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("99");
    }

    @Test
    void 落库异常_返回失败提示_不置终态() {
        when(gameStateService.getQuestion("5/100")).thenReturn(null);
        when(turtleSoupService.saveTurtleSoup(any())).thenThrow(new RuntimeException("db down"));

        String reply = tool.saveGameResult("5/100", "标题", "真相");

        assertThat(reply).contains("保存失败");
        verify(gameStateService, never()).markSolved(any());
    }
}
