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

`GameAgent` (`backend/src/main/java/com/weidong/gamebackend/assistant/GameAgent.java`) is a LangChain4j `@AiService` interface — it is the entire game host. Its system message is `src/main/resources/haiguitang-prompt-template.txt`, which defines all gameplay rules (yes/no/irrelevant answers only, two-level hint system, when the game ends). There is no game-state code; changing game behavior means editing the prompt template.

Key contract between prompt and frontend: when the game ends, the LLM must output a response whose **first line is exactly `游戏结束`**. `HomeView.vue` detects the end of game with `text.includes("游戏结束")` — never break this string in the prompt or the UI.

### Chat flow

1. Frontend generates `memoryId = "${userId}/${timestamp}"` (in `HomeView.vue`) and sends `PUT /chat` with `{memoryId, message}` (validated via `@Valid` `dto/ChatRequest`).
2. `GameController` delegates to `GameAgent.chat(memoryId, message)` — a synchronous call returning the raw AI reply as a plain `String` (no SSE/streaming).
3. `GameAgentConfig` provides a `ChatMemoryProvider`: per-memoryId `MessageWindowChatMemory` with max 60 messages, in-memory only (Redis planned).
4. When the player guesses correctly, the LLM itself invokes the `saveGameResult` tool (`tool/GameResultTool.java`, registered via `@AiService(tools=...)`) which parses `userId` from the `memoryId` prefix and inserts a row into `completed_games` via `TurtleSoupService` (MyBatis-Plus `IService`, no XML mapper).
5. Frontend forces a reveal after 30 user messages by sending "猜不出来，公布答案".

### Web layer conventions

- Every endpoint returns `Result<T>{code,message,data}` (`common/Result.java`); `code=0` success, `4xxxx` client errors, `5xxxx` server errors (`common/ErrorCode.java`).
- Business errors are thrown as `BusinessException` and converted by `GlobalExceptionHandler` (`@RestControllerAdvice`); validation errors and unexpected exceptions are also handled there — controllers contain no try-catch.
- Requests use `dto/*Request` (`@NotBlank` etc.); responses return `vo/TurtleSoupVO` (hides userId/roomId). List endpoints return `PageResult<T>{records,total,pageNum,pageSize}` backed by MyBatis-Plus `Page` + `PaginationInnerInterceptor` (`configuration/MybatisPlusConfig.java`).
- SpringDoc UI at `/swagger-ui.html` (dev only).

### REST endpoints

- `PUT /chat` — chat with the AI host (unified Result)
- `GET /turtle-soups?pageNum=1&pageSize=10` — paginated completed-game records
- `GET /turtle-soups/{userId}` — completed games of one user
- `POST /turtle-soups` — create a record (validated `TurtleSoupCreateRequest`)

### Frontend structure

- `src/main.js` — creates/reads a UUID in `localStorage` as `gameUserId`, exposed as global `window.gameUserId` (views read it directly; no auth).
- Routes: `/home` (game, `HomeView.vue`), `/profile` (completed games, `ProfileView.vue`); `Sidebar.vue` in `App.vue`.
- `@` alias resolves to `src/` (configured in both `vite.config.js` and `jsconfig.json`).
- Views parse the unified `Result` envelope: `result.code === 0` → use `result.data`, else show `result.message`.

### Gotchas

- Backend URL `http://localhost:8080` is hardcoded in `HomeView.vue` and `ProfileView.vue` — there is no API client abstraction or Vite proxy. Backend CORS (`CorsConfig.java`) only allows `http://localhost:5173`.
- `GameResultTool.saveGameResult` is called by the LLM with free-form `title`/`solution` arguments; `memoryId` must contain `/` or the save is rejected.
- Version pins (rationale in pom comments): MyBatis-Plus locked at 3.5.9 (3.5.17 restructured packages; 3.5.9 needs explicit `mybatis-plus-extension` + `mybatis-plus-jsqlparser` modules for Page/分页插件); SpringDoc locked at 2.8.x (3.x pulls Spring Boot 4 modules, causing `conventionErrorViewResolver` duplicate-registration startup failure); LangChain4j spring starters only exist as betas.
- Test coverage is minimal (a single `contextLoads` test); the Spring context pulls in the datasource and LangChain4j config, so tests need a running MySQL and `DB_PASSWORD`.
