package com.weidong.gamebackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weidong.gamebackend.service.GameStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * /chat 接口鉴权与题库开局集成测试。
 * 不触发真实 LLM 调用: 鉴权/校验分支在进入模型前抛出；题库开局由后端直出汤面。
 */
@SpringBootTest
@AutoConfigureMockMvc
class GameControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private GameStateService gameStateService;
    @Autowired
    private StringRedisTemplate redisTemplate;

    /** 注册随机用户，返回 data 节点（accessToken/userId） */
    private JsonNode registerUser() throws Exception {
        String username = "it_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"pass123456\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private String memoryIdOf(long userId) {
        return userId + "/" + System.currentTimeMillis();
    }

    @Test
    void 未登录_直接拒绝() throws Exception {
        mockMvc.perform(put("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memoryId\":\"1/1\",\"message\":\"开始游戏\"}"))
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void 伪造他人会话memoryId_被拒() throws Exception {
        JsonNode user = registerUser();
        mockMvc.perform(put("/chat")
                        .header("Authorization", "Bearer " + user.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memoryId\":\"999999/1\",\"message\":\"开始游戏\"}"))
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void 已结束会话_拒绝继续对话() throws Exception {
        JsonNode user = registerUser();
        String memoryId = memoryIdOf(user.get("userId").asLong());
        gameStateService.markSolved(memoryId);
        try {
            mockMvc.perform(put("/chat")
                            .header("Authorization", "Bearer " + user.get("accessToken").asText())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"memoryId\":\"" + memoryId + "\",\"message\":\"继续提问\"}"))
                    .andExpect(jsonPath("$.code").value(40000));
        } finally {
            redisTemplate.delete("hgt:game-state:" + memoryId);
        }
    }

    @Test
    void 开始游戏_题库开局直出题目与情境() throws Exception {
        JsonNode user = registerUser();
        String memoryId = memoryIdOf(user.get("userId").asLong());
        try {
            MvcResult result = mockMvc.perform(put("/chat")
                            .header("Authorization", "Bearer " + user.get("accessToken").asText())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"memoryId\":\"" + memoryId + "\",\"message\":\"开始游戏\"}"))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            MvcResult dispatched = mockMvc.perform(asyncDispatch(result)).andReturn();
            // SSE 响应无 charset 声明，手动按 UTF-8 解码避免中文断言乱码
            String content = dispatched.getResponse().getContentAsString(StandardCharsets.UTF_8);
            assertThat(content).contains("【题目】").contains("【情境】")
                    .contains("\"state\":\"RUNNING\"");
        } finally {
            redisTemplate.delete("hgt:game-state:" + memoryId);
            redisTemplate.delete("hgt:game-question:" + memoryId);
        }
    }
}
