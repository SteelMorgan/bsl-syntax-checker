<!-- fbf6e741-06c7-4a39-8ffd-22f9907357a5 92cb6972-b5f0-4adb-9ba8-8840376d49c1 -->
### MCP BSL Server (Spring) — Implementation Plan

#### Sources and References

- BSL Language Server docs: [link](https://1c-syntax.github.io/bsl-language-server/en/)
- GitHub repo and releases: [link](https://github.com/1c-syntax/bsl-language-server)

#### Architecture

- Core modules
  - `bsl-adapter`: manage BSL LS processes (LSP/WS for low-latency pool; CLI analyze/format for batch). JVM flags: `-Xmx4g` (configurable).
  - `transports`: stdio, HTTP (REST), SSE, NDJSON.
  - `presentation`: REST controllers + stdio dispatcher + Swagger UI + status page.
  - `logging`: Logback → Loki (toggleable on/off, default ON) + Grafana proxy `/grafana`.
- Path mapping
  - Host root mount: configurable. Build-time ARG `MOUNT_HOST_ROOT` (default empty), runtime ENV `MOUNT_HOST_ROOT` (recommended). Example: `D:\My Projects\Projects 1C` → `/workspaces` (read-only).
  - Validate and translate host paths only when `MOUNT_HOST_ROOT` is set; reject paths outside mapped root. If not set, absolute-path tools return validation error.
- Session pool
  - Key: normalized container project path.
  - Lifecycle: LRU with TTL + max pool size (configurable). Warm-up on first request.

#### Transports

- stdio (JSON lines): request/response typed JSON, single-tool mode per message.
- HTTP (REST): JSON in/out.
- SSE: server push for long-running diagnostics/progress.
- NDJSON: chunked JSON for streaming results over HTTP when SSE not used.

#### HTTP Endpoints (minimal)

- `POST /api/analyze` (sync or streamed)
- `POST /api/format` (sync)
- `POST /api/session/start`, `GET /api/session/status`, `POST /api/session/stop`
- `GET /api/diagnostics/stream` (SSE)
- `GET /status`, `GET /actuator/health`, Swagger: `/swagger-ui`, `/v3/api-docs`

#### MCP Tools (JSON, concise English)

- Tool: `bslcheck_analyze`
  - Request (body): `{ "srcDir": "<hostAbsolutePath>", "reporters": ["json"], "language": "ru|en", "stream": true|false }`
  - Response (sync): `{ "summary": {"errors": n, "warnings": n}, "diagnostics": [ {"file": "...", "line": n, "code": "...", "severity": "error|warning", "message": "..." } ] }`
  - Stream (SSE/NDJSON): emits `{ "event": "diagnostic", "data": { ...diagnostic } }` and final `{ "event": "summary", "data": { ... } }`
  - Critical: `srcDir` must be under mounted root; path translated to `/workspaces/...`.
- Tool: `bslcheck_format`
  - Request: `{ "src": "<hostAbsPath to dir|file>", "inPlace": true|false }`
  - Response: `{ "formatted": true, "filesChanged": n }` or NDJSON per file `{ "file": "...", "changed": true }`
  - Critical: if `inPlace=false`, server returns temp artifact link (HTTP) instead of modifying files.
- Tool: `bslcheck_session_start`
  - Request: `{ "projectPath": "<hostAbsPath>" }`
  - Response: `{ "sessionId": "uuid", "project": "<normalized>" }`
- Tool: `bslcheck_session_status`
  - Request: `{ "sessionId": "uuid" }`
  - Response: `{ "status": "ready|starting|stopped", "uptimeSec": n }`
- Tool: `bslcheck_session_stop`
  - Request: `{ "sessionId": "uuid" }`
  - Response: `{ "stopped": true }`

#### Logging and Monitoring

- Logback → Loki (HTTP appender), toggle by `LOGGING_ENABLED=true|false` (default true).
- Grafana served via reverse proxy `/grafana` (iframe in web UI).
- Metrics: Spring Actuator + Micrometer (Prometheus exporter optional).

#### BSL LS Delivery

- At Docker build: ARG `BSL_VERSION=latest`; if `GITHUB_TOKEN` present, fetch latest release asset; else pin a specific version.
- Store JAR under `/opt/bsl/bsl-language-server.jar`.

#### Docker

- Image based on JRE 17.
- Configurable ports: ENV `SERVER_PORT` (container, default 8080), ENV `HOST_PORT` (host, default 8080). Spring: `server.port=${SERVER_PORT:8080}`.
- Run (PowerShell):
  - `$env:SERVER_PORT=8080; $env:HOST_PORT=18080; docker run --rm -e SERVER_PORT=$env:SERVER_PORT -p $env:HOST_PORT`:`$env:SERVER_PORT` -v 'D:\My Projects\Projects 1C:/workspaces:ro' mcp-bsl`
- SSE/WS share the same port; stdio via dedicated entrypoint.

#### Security

- No auth (internal use). CORS: allow configured origins. Input validation on paths.

#### Secrets Management

- GitHub token stored in `.secrets/github_token.txt` (UTF-8, single line).
- `.gitignore` includes: `.secrets/`, `secrets/`, `.env`.
- Build: `$env:GITHUB_TOKEN = Get-Content '.secrets/github_token.txt' -Raw; docker build --build-arg GITHUB_TOKEN=$env:GITHUB_TOKEN -t mcp-bsl .`

#### Risks/Limits

- Memory for large projects → tune `-Xmx`.
- Pool size/TTL to avoid idle resource usage.

#### Quality Gates

- Do not proceed to the next stage until the current one passes verification.
- Each TODO must define a minimal verification: build/tests/health/API probe/logs.
- CI gating order: build → unit → integration → e2e/stdio/http → container run (health) → publish.

### To-dos

- [ ] Инициализировать Spring-проект и базовые конфиги (Actuator, Swagger, logging toggle)
- [ ] Реализовать адаптер BSL LS: пул сессий, CLI analyze/format, JVM flags
- [ ] Реализовать stdio, HTTP REST, SSE, NDJSON роуты/диспетчер
- [ ] Добавить валидацию и трансляцию путей хоста → контейнера
- [ ] Добавить OpenAPI схемы и Swagger UI
- [ ] Подключить Logback Loki appender и прокси Grafana UI
- [ ] Страница статуса сервера и health endpoints

