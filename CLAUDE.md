# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

海龟汤 (Turtle Soup) — a lateral-thinking puzzle game where an LLM (qwen-max via Alibaba DashScope) acts as the game host. Two independent apps in one repo:

- `game-backend/` — Spring Boot 3.5.16 (Java 17, Maven) REST API + LangChain4j AI agent + MyBatis-Plus/MySQL + Flyway
- `game-frontend/` — Vue 3 + Vite 7 + Element Plus SPA (Chinese UI)

The codebase, comments, prompt templates, and UI text are in Chinese — keep user-facing strings in Chinese.

## Commands

All commands run from the respective subdirectory. The machine's default JAVA_HOME is JDK 8 — prefix backend commands with `JAVA_HOME="/c/Program Files/Java/jdk-17"` (Git Bash syntax).

### Backend (`game-backend/`)

```sh
./mvnw spring-boot:run   # start server on port 8080
./mvnw test               # run all tests
./mvnw -Dtest=GameBackendApplicationTests test   # run a single test class
./mvnw package            # build
```

- Requires JDK 17 and env vars: `DASH_SCOPE_API_KEY` (Alibaba Bailian API key, in system env) and `DB_PASSWORD` (local MySQL root password — not in system env by default; pass inline, e.g. `DB_PASSWORD=xxx ./mvnw test`).
- Local MySQL (Windows service `MySQL80`) with database `haiguitang`. Schema is created/migrated by Flyway on startup (`db/migration/V1__init.sql`; `baseline-on-migrate` for pre-existing dev DBs).
- Dev profile is the default (`${SPRING_PROFILES_ACTIVE:dev}`); prod profile takes all datasource config from env vars and disables SpringDoc. Note: an env var set to an EMPTY string overrides the `:dev` default and breaks startup.

### Frontend (`game-frontend/`)

```sh
npm install
npm run dev       # dev server on port 5173 (CORS on backend allows only this origin)
npm run build
npm run preview
```

Node `^20.19.0 || >=22.12.0` required. No test or lint setup exists.

## Architecture

### The game logic lives in the LLM prompt, not in code

`GameAgent` (`backend/src/main/java/com/weidong/gamebackend/assistant/GameAgent.java`) is a LangChain4j `@AiService` interface — it is the entire game host. Its system message is `src/main/resources/haiguitang-prompt-template.txt`, which defines all gameplay rules (yes/no/irrelevant answers only, two-level hint system, when the game ends). Changing gameplay behavior means editing the prompt template; **game termination is a backend state machine** (below).

Key contract: the backend drives game end via `GameStateService` (Redis, `hgt:game-state:{memoryId}`, TTL 7d): `RUNNING` → `SOLVED` (猜对，GameResultTool 落库成功) or `REVEALED` (揭晓，结构化输出落库成功). The SSE `done` event carries `state`; `HomeView.vue` ends the game when `state !== "RUNNING"`. The "游戏结束" first line in AI text is now **display convention only** (kept for UX) — no code parses it.

### Auth (W2 用户体系)

- 注册/登录 (`POST /auth/register`、`/auth/login`) 返回双 token: access 30 分钟 + refresh 7 天（JJWT 0.12 签发，HS256，密钥 `JWT_SECRET` 环境变量注入，dev 有默认值）。
- `JwtAuthenticationFilter` (`security/`) 解析 `Authorization: Bearer` 并把 `LoginUser` 放入 SecurityContext；`SecurityConfig` 无状态配置（关闭 Session/CSRF），`/auth/**` 与 swagger 放行，其余接口需认证。401/403 由 `RestAuthenticationEntryPoint`/`RestAccessDeniedHandler` 输出统一 Result JSON（40100/40300）。
- refresh 不轮换（无状态 JWT 无法作废旧 token，Redis 上线后升级）；密码 BCrypt；登录失败统一提示防用户名枚举。

### Chat flow

1. 前端生成 `memoryId = "${userId}/${timestamp}"`（`HomeView.vue`，userId 来自登录态）并发送 `PUT /chat`（`@Valid` `dto/ChatRequest`）；`GameController` 校验 memoryId 前缀与登录用户一致，防止伪造他人会话；Redis 状态机已终态的会话拒绝继续对话。
2. `GameController` 按消息分流:
   - 开局（消息含"开始游戏"）→ **题库抽题**（`SoupQuestionService.pick`）: 排除该用户已玩过的题（`completed_games.question_id` 关联，同一题可给不同用户重复用），新玩家（完成 0~2 局）优先简单题；抽中则汤面由后端直接按【题目】/【情境】输出（不经 LLM、省一次调用），汤底作为系统消息注入该会话聊天记忆（主持人每轮可见答案键，玩家不可见），`hgt:game-question:{memoryId}` 记录 questionId 供落库关联。**该用户玩遍题库时回退 LLM 即兴出题**（题库是质量下限与成本优化，不是架构瓶颈）。
   - 揭晓类消息（`公布答案|揭晓答案|结束游戏|猜不出来|放弃|不玩了`）→ **`GameRevealAgent.reveal`（同步 + 结构化输出）**: 类型化返回 `GameReveal{title, solution, reply}`（LangChain4j 类型化返回自动走 JSON 模式），业务代码自行落库 + 置状态 `REVEALED`——揭晓不依赖 LLM 自主调用工具，根治"只恭喜不落库"。系统消息 `haiguitang-reveal-template.txt`（独立于主 prompt）。题库局会把汤底注入揭晓请求，保证长对局下揭晓内容仍准确。
   - 常规消息 → `GameAgent.chatStream`（流式）: SSE `token/done/error` 事件，`done` 携带状态机终态。
3. `GameAgentConfig` 提供 `ChatMemoryProvider`: per-memoryId `MessageWindowChatMemory`，最多 80 条（60 条问答 + 开局注入的答案键 + 工具返回值，保证答案键整局不被窗口淘汰），`RedisChatMemoryStore` 持久化（`hgt:chat-memory:{memoryId}`，TTL 7 天）；`GameAgent` 与 `GameRevealAgent` 共享该 provider（同一 memoryId 同一历史）。
4. 玩家猜对时 LLM 自主调用 `saveGameResult` 工具（`tool/GameResultTool.java`）: userId 优先从 SecurityContext 取；SSE 流式下工具在模型 SDK 回调线程执行、ThreadLocal 安全上下文不传播，回退为解析 memoryId 前缀（入口已校验归属，不经过 LLM）。题库局忽略 LLM 传参、直接用库里的标题/汤底落库（记录确定性）。落库成功后置状态 `SOLVED`。
5. 前端在 30 条消息后自动发送"猜不出来，公布答案"（后端识别后走揭晓分支）。
6. token 用量: 流式路径在 `onCompleteResponse` 聚合落库（每次 /chat 一条）；揭晓同步路径由 `SyncUsageListener`（ChatModelListener + `SyncUsageContext` ThreadLocal）按模型调用落库，两条路径互不重复。

### Web layer conventions

- Every endpoint returns `Result<T>{code,message,data}` (`common/Result.java`); `code=0` success, `4xxxx` client errors, `5xxxx` server errors (`common/ErrorCode.java`).
- Business errors are thrown as `BusinessException` and converted by `GlobalExceptionHandler` (`@RestControllerAdvice`); validation errors and unexpected exceptions are also handled there — controllers contain no try-catch.
- Requests use `dto/*Request` (`@NotBlank` etc.); responses return `vo/TurtleSoupVO` (hides userId/roomId). List endpoints return `PageResult<T>{records,total,pageNum,pageSize}` backed by MyBatis-Plus `Page` + `PaginationInnerInterceptor` (`configuration/MybatisPlusConfig.java`).
- SpringDoc UI at `/swagger-ui.html` (dev only).

### REST endpoints

- `POST /auth/register` — 注册（注册即登录，返回令牌对 + userId/username）
- `POST /auth/login` — 登录，返回 access/refresh 令牌对
- `POST /auth/refresh` — refresh 换新 accessToken
- `PUT /chat` — chat with the AI host（需认证，校验 memoryId 归属）
- `GET /turtle-soups?pageNum=1&pageSize=10` — 分页查询**当前登录用户**的完成记录
- `POST /turtle-soups` — create a record（userId 从登录态取，请求体不含 userId）

### Frontend structure

- `src/api/http.js` — axios 实例: baseURL 走 `VITE_API_BASE_URL`（`.env.development` 指向 localhost:8080）；请求拦截器带 Bearer；响应拦截器解包 `Result`、统一 ElMessage 报错、401 自动 refresh 重放（并发锁防重复刷新）。
- `src/utils/auth.js` — token 与用户信息存取（localStorage: hgt_access_token / hgt_refresh_token / hgt_user）。
- 路由守卫 (`router/index.js`): 未登录访问受限页跳 `/login`；已登录访问 `/login` 跳首页。
- Routes: `/home` (game, `HomeView.vue`), `/profile` (completed games, `ProfileView.vue`), `/login` (`AuthView.vue`, 登录/注册); `Sidebar.vue`（含用户名与退出登录）只在非登录页显示（`App.vue` 控制）。
- `@` alias resolves to `src/` (configured in both `vite.config.js` and `jsconfig.json`).
- 拦截器已解包 Result，视图里 `await http.get(...)` 直接拿到 `data`，无需再判断 code。

### Gotchas

- CORS 必须注册在 **Security 层**（`CorsConfig` 提供 `CorsConfigurationSource` bean + `http.cors(withDefaults())`）: 401/403 由 Security 过滤器直接写出、不经过 MVC，只配 `WebMvcConfigurer` 时这些响应无 CORS 头，浏览器报 CORS 错误且前端拿不到 401 状态码，自动刷新链路失效。
- LangChain4j 1.20.0-beta30 的 `@V` 参数传播**不生效**（参数会暴露给 LLM 被自由发挥），勿使用；用户身份一律从 SecurityContext 取，不经过 LLM。
- 猜对路径的 `GameResultTool.saveGameResult` 仍由 LLM 自主决定调用、title/solution 由 LLM 填写——若 LLM "只恭喜不调用工具"，游戏不结束但**记录最终必落库**（玩家放弃走揭晓分支，由业务代码落库）；揭晓路径已完全不依赖工具（W3 结构化输出）。
- 结构化输出（`GameReveal` 类型化返回）在 1.20.0-beta30 只支持非流式模型，故揭晓走独立的同步 `GameRevealAgent`（qwenChatModel），流式与结构化输出不可混在同一 `@AiService`。
- Version pins (rationale in pom comments): MyBatis-Plus locked at 3.5.9 (3.5.17 restructured packages; 3.5.9 needs explicit `mybatis-plus-extension` + `mybatis-plus-jsqlparser` modules for Page/分页插件); SpringDoc locked at 2.8.x (3.x pulls Spring Boot 4 modules, causing `conventionErrorViewResolver` duplicate-registration startup failure); LangChain4j spring starters only exist as betas.
- Tests (W4): 单元测试（`tool/GameResultToolTest`、`service/SoupQuestionServiceTest`、`service/GameStateServiceTest`）纯 Mockito，无需任何环境；集成测试（`controller/AuthFlowIntegrationTest`、`controller/GameControllerIntegrationTest`）走 @SpringBootTest + MockMvc 真实 Security 链，需要运行中的 MySQL + Redis 以及 `DB_PASSWORD`/`DASH_SCOPE_API_KEY` 环境变量，会向 dev 库注册 `it_` 前缀的随机测试用户（不清理），不触发真实 LLM 调用（鉴权/校验在进入模型前抛出，题库开局由后端直出汤面）。
