# MCP BSL Server Architecture

## Overview

MCP BSL Server реализует **Dual-Port Architecture** для разделения Web UI и MCP API на независимых портах.

## Dual-Port Design

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     MCP BSL Server Container                 │
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │          Spring Boot Application                      │   │
│  │                                                        │   │
│  │  ┌────────────────────┐    ┌─────────────────────┐  │   │
│  │  │   Web UI Server    │    │   MCP Transport     │  │   │
│  │  │   (Port 9090)      │    │   (Port 8080)       │  │   │
│  │  │   ALWAYS ACTIVE    │    │   CONDITIONAL       │  │   │
│  │  │                    │    │                     │  │   │
│  │  │ • Swagger UI       │    │ • stdio (stdin/out) │  │   │
│  │  │ • Actuator         │    │ • HTTP REST API     │  │   │
│  │  │ • Prometheus       │    │ • SSE streaming     │  │   │
│  │  │ • Status Page      │    │ • NDJSON chunking   │  │   │
│  │  └────────────────────┘    └─────────────────────┘  │   │
│  │                                                        │   │
│  │  ┌────────────────────────────────────────────────┐  │   │
│  │  │       Core Services Layer                      │  │   │
│  │  │                                                 │  │   │
│  │  │  • BslSessionPool   (LSP session management)  │  │   │
│  │  │  • BslCliService    (analyze/format)          │  │   │
│  │  │  • PathMapping      (host↔container paths)     │  │   │
│  │  │  • LoggingService   (Loki integration)        │  │   │
│  │  └────────────────────────────────────────────────┘  │   │
│  │                                                        │   │
│  │  ┌────────────────────────────────────────────────┐  │   │
│  │  │       BSL Language Server (JAR)                │  │   │
│  │  │  /opt/bsl/bsl-language-server.jar (104MB)     │  │   │
│  │  └────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                               │
│  Volumes:                                                     │
│    /workspaces  ← D:\My Projects\Projects 1C (mounted)      │
└─────────────────────────────────────────────────────────────┘
```

### Port Allocation

| Port | Service | Status | Purpose |
|------|---------|--------|---------|
| **9090** | Web UI | Always Active | Management, monitoring, documentation |
| **8080** | MCP API | Conditional | MCP transport (only for http/sse/ndjson modes) |

### Transport Modes

#### 1. **stdio** (default, recommended for Cursor IDE)
```yaml
MCP_TRANSPORT: stdio
```
- **Web UI Port**: 9090 (active)
- **MCP Port**: N/A (uses stdin/stdout)
- **Use case**: Cursor IDE integration, local development
- **Communication**: JSON-RPC over stdin/stdout

#### 2. **http** (synchronous REST API)
```yaml
MCP_TRANSPORT: http
MCP_PORT: 8080
```
- **Web UI Port**: 9090 (active)
- **MCP Port**: 8080 (active)
- **Use case**: Remote clients, testing with Swagger, debugging
- **Communication**: Synchronous JSON over HTTP

#### 3. **sse** (Server-Sent Events, planned)
```yaml
MCP_TRANSPORT: sse
MCP_PORT: 8080
```
- **Web UI Port**: 9090 (active)
- **MCP Port**: 8080 (active)
- **Use case**: Real-time streaming for large projects
- **Communication**: Event stream over HTTP

#### 4. **ndjson** (Newline-Delimited JSON, planned)
```yaml
MCP_TRANSPORT: ndjson
MCP_PORT: 8080
```
- **Web UI Port**: 9090 (active)
- **MCP Port**: 8080 (active)
- **Use case**: Chunked responses, low memory overhead
- **Communication**: NDJSON over HTTP

## Web UI (Port 9090)

**Always active, regardless of MCP transport mode.**

### Endpoints

| Endpoint | Description |
|----------|-------------|
| `/` | Status dashboard (HTML) |
| `/status` | Server status (JSON) |
| `/swagger-ui/index.html` | API documentation |
| `/actuator/health` | Health check |
| `/actuator/prometheus` | Prometheus metrics |
| `/actuator/info` | Build info |

### Purpose
- **Monitoring**: Real-time server status, health checks
- **Documentation**: Swagger UI for API exploration
- **Metrics**: Prometheus endpoint for Grafana dashboards
- **Debugging**: Interactive API testing

## MCP API (Port 8080)

**Active only when `MCP_TRANSPORT` is http, sse, or ndjson.**

### Endpoints (HTTP mode)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/analyze` | POST | Analyze BSL code |
| `/api/format` | POST | Format BSL code |
| `/api/session/start` | POST | Start LSP session |
| `/api/session/status` | GET | Get session status |
| `/api/session/stop` | POST | Stop LSP session |

## Configuration Properties

### Application (application.yml)

```yaml
# Web UI Server (ALWAYS active)
server:
  port: ${WEB_UI_PORT:9090}

# MCP Transport configuration
mcp:
  transport: ${MCP_TRANSPORT:stdio}  # stdio, http, sse, ndjson
  port: ${MCP_PORT:8080}             # Only for http/sse/ndjson
  
# BSL Language Server
bsl:
  jar-path: ${BSL_JAR_PATH:/opt/bsl/bsl-language-server.jar}
  jvm:
    max-heap: ${BSL_MAX_HEAP:4g}
  pool:
    max-size: ${BSL_POOL_MAX_SIZE:5}
    ttl-minutes: ${BSL_POOL_TTL:60}

# Path mapping (Docker volume)
path:
  mount:
    host-root: ${MOUNT_HOST_ROOT:}
    container-root: /workspaces

# Logging
logging:
  loki:
    enabled: ${LOGGING_ENABLED:true}
    url: ${LOKI_URL:http://localhost:3100/loki/api/v1/push}
```

## Deployment Scenarios

### Scenario 1: Cursor IDE (stdio mode)

```powershell
docker run --rm -i `
  --network host `
  -v 'D:\My Projects\Projects 1C:/workspaces:ro' `
  -e MCP_TRANSPORT=stdio `
  mcp-bsl-server:latest
```

**Result**:
- MCP: stdin/stdout (Cursor IDE communication)
- Web UI: http://localhost:9090 (monitoring/debugging)

### Scenario 2: Remote HTTP API

```powershell
docker run --rm -d `
  --name mcp-bsl-http `
  -p 9090:9090 `
  -p 8080:8080 `
  -v 'D:\My Projects\Projects 1C:/workspaces:ro' `
  -e MCP_TRANSPORT=http `
  mcp-bsl-server:latest
```

**Result**:
- MCP API: http://localhost:8080 (client access)
- Web UI: http://localhost:9090 (monitoring/debugging)

### Scenario 3: Development with Docker Compose

```yaml
services:
  mcp-bsl-server:
    image: mcp-bsl-server:latest
    ports:
      - "9090:9090"  # Web UI
      - "8080:8080"  # MCP API
    environment:
      MCP_TRANSPORT: http
      LOGGING_ENABLED: true
    volumes:
      - ./testdata:/workspaces:ro
```

**Result**:
- Full stack with Loki + Grafana + Prometheus
- Both ports exposed for development

## Security

### Current Implementation
- **No authentication/authorization** (as per requirements)
- **Read-only volume mount** (`:ro` flag)
- **No secrets in logs**

### Future Enhancements (Optional)
- JWT authentication for HTTP mode
- Rate limiting
- CORS configuration
- TLS/SSL support

## Performance

### Resource Usage
- **Base image**: eclipse-temurin:17-jre-alpine (~180MB)
- **BSL LS JAR**: ~104MB
- **Total image**: ~350MB
- **Runtime memory**: 512MB (app) + 4GB (BSL LS pool)

### Optimization
- **Session pooling**: Reuse LSP instances (low latency)
- **Path caching**: Host→container path translation
- **Lazy initialization**: BSL LS starts on first request

## Monitoring Stack

```
┌─────────────────┐
│  MCP BSL Server │
│  (Port 9090)    │
└────────┬────────┘
         │
         ├─── Logs ────────► Loki (Port 3100)
         │                     │
         │                     └──► Grafana (Port 3000)
         │
         └─── Metrics ─────► Prometheus (Port 9090)
                               │
                               └──► Grafana (Port 3000)
```

### Grafana Access
- **URL**: http://localhost:3000
- **Default credentials**: admin / admin
- **Datasources**: Loki (logs) + Prometheus (metrics)

## Development Workflow

1. **Local development** (no Docker):
   ```powershell
   .\gradlew.bat bootRun
   ```
   - Web UI: http://localhost:9090
   - Hot reload: enabled

2. **Docker build**:
   ```powershell
   .\gradlew.bat build
   docker build -t mcp-bsl-server:latest .
   ```

3. **Integration testing**:
   ```powershell
   docker-compose up -d
   ```
   - Full stack with monitoring

4. **Cursor IDE integration**:
   - Update `mcp-config.json`
   - Restart Cursor IDE

## Troubleshooting

### Port Conflicts

**Problem**: Port 9090 already in use
```
Error starting server: Port 9090 is already in use
```

**Solution**: Change Web UI port
```powershell
docker run -p 9091:9090 -e WEB_UI_PORT=9091 ...
```

### BSL LS Download Fails

**Problem**: GitHub rate limit
```
403 Forbidden: API rate limit exceeded
```

**Solution**: Use GitHub Personal Access Token
```powershell
# Store token in .secrets/github_token.txt
$env:GITHUB_TOKEN = Get-Content '.secrets\github_token.txt'
docker build --build-arg GITHUB_TOKEN=$env:GITHUB_TOKEN ...
```

### Path Mapping Issues

**Problem**: "Path not found in mounted volume"
```json
{
  "error": "Path D:\\Projects\\test.bsl is not within mounted volume"
}
```

**Solution**: Verify volume mount
```powershell
# Check mount path
docker inspect mcp-bsl-server | Select-String "Mounts" -Context 0,10

# Correct mount example
-v 'D:\Projects:/workspaces:ro'
```

## Future Enhancements

### Planned Features
1. **SSE Transport**: Real-time streaming for large analysis results
2. **NDJSON Transport**: Memory-efficient chunked responses
3. **WebSocket Transport**: Bidirectional communication
4. **Grafana Proxy**: `/grafana` endpoint on Web UI
5. **Authentication**: JWT-based access control

### Roadmap
- **v0.2.0**: SSE + NDJSON transports
- **v0.3.0**: WebSocket transport
- **v0.4.0**: Authentication & authorization
- **v1.0.0**: Production-ready release

## References

- [BSL Language Server](https://github.com/1c-syntax/bsl-language-server)
- [Model Context Protocol (MCP)](https://modelcontextprotocol.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Loki](https://grafana.com/oss/loki/)
- [Prometheus](https://prometheus.io/)
