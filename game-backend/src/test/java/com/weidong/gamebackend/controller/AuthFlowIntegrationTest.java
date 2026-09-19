package com.weidong.gamebackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 用户体系全链路集成测试（真实 MySQL/Redis + 完整 Spring Security 过滤链）。
 * 每次运行注册一个随机用户名，不清理数据库（it_ 前缀便于识别测试数据）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String newUsername() {
        return "it_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String register(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    @Test
    void 注册登录刷新_携带token访问受限接口_全链路() throws Exception {
        String username = newUsername();
        String password = "pass123456";

        // 1. 注册即登录，返回双 token
        MvcResult register = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();
        JsonNode data = objectMapper.readTree(register.getResponse().getContentAsString()).get("data");
        String accessToken = data.get("accessToken").asText();
        String refreshToken = data.get("refreshToken").asText();
        long userId = data.get("userId").asLong();
        assertThat(userId).isPositive();

        // 2. 重复注册被拒
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(jsonPath("$.code").value(40001));

        // 3. 密码错误登录失败（统一提示，防用户名枚举）
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"wrong-password\"}"))
                .andExpect(jsonPath("$.code").value(40002));

        // 4. 正确登录
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(jsonPath("$.code").value(0));

        // 5. 携带 access token 访问受限接口（记录分页）
        mockMvc.perform(get("/turtle-soups")
                        .param("pageNum", "1").param("pageSize", "5")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.code").value(0));

        // 6. refresh 换新 access，用户身份不变
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.userId").value(userId));
    }

    @Test
    void 伪造或篡改token被拒() throws Exception {
        mockMvc.perform(get("/turtle-soups")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(jsonPath("$.code").value(40100));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"garbage-token\"}"))
                .andExpect(jsonPath("$.code").value(40003));
    }

    @Test
    void 未携带token访问受限接口_返回401() throws Exception {
        mockMvc.perform(get("/turtle-soups"))
                .andExpect(jsonPath("$.code").value(40100));
    }
}
