# Transport Protocols

MCP BSL Server supports multiple transport protocols for different use cases.

## Supported Transports

| Transport | Use Case | Implementation Status |
|-----------|----------|----------------------|
| **stdio** | Cursor IDE integration | ✅ Implemented |
| **HTTP REST** | Web API, external tools | ✅ Implemented |
| **SSE** | Streaming diagnostics | ✅ Implemented |
| **NDJSON** | Chunked responses | ✅ Implemented |

## 1. STDIO (Standard Input/Output)

**Purpose**: Primary transport for MCP clients like Cursor IDE.

**Format**: Newline-delimited JSON (JSON Lines).

### Request Format

Each request is a JSON object on a single line:

```json
{"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"analyze","arguments":{"srcDir":"D:\\Projects\\MyProject"}}}
```

### Response Format

Responses are JSON objects on separate lines:

```json
{"jsonrpc":"2.0","id":1,"result":{"status":"success","data":{"summary":{"errors":5,"warnings":12}}}}
```

### Usage

**Start Server**:
```bash
set TRANSPORT_MODE=stdio
java -jar build\libs\mcp-bsl-server-0.1.0-SNAPSHOT.jar
```

The server will:
1. Read JSON requests from `stdin`
2. Write JSON responses to `stdout`
3. Write logs to `stderr` (if logging enabled)

**Example Communication**:

```bash
# Input (stdin)
{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}

# Output (stdout)
{"jsonrpc":"2.0","id":1,"result":{"tools":[{"name":"analyze","description":"..."},{"name":"format","description":"..."}]}}

# Input
{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"analyze","arguments":{"srcDir":"D:\\Projects\\Test"}}}

# Output
{"jsonrpc":"2.0","id":2,"result":{"status":"success","data":{"summary":{"errors":0,"warnings":3},"diagnostics":[...]}}}
```

### Error Handling

Errors are returned in JSON-RPC 2.0 format:

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "error": {
    "code": -32602,
    "message": "Invalid params: Path outside mounted root",
    "data": {
      "path": "C:\\Windows\\System32",
      "mountRoot": "D:\\Projects\\1C"
    }
  }
}
```

**Standard Error Codes**:
- `-32700`: Parse error
- `-32600`: Invalid request
- `-32601`: Method not found
- `-32602`: Invalid params
- `-32603`: Internal error

## 2. HTTP REST API

**Purpose**: Direct HTTP access for web clients, testing, external integrations.

**Base URL**: `http://localhost:8080/api`

### Endpoints

#### POST `/api/analyze`

Analyze BSL source code.

**Request**:
```http
POST /api/analyze HTTP/1.1
Content-Type: application/json

{
  "srcDir": "D:\\Projects\\MyProject",
  "reporters": ["json"],
  "language": "ru"
}
```

**Response (Success)**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "status": "success",
  "data": {
    "summary": {
      "errors": 5,
      "warnings": 12
    },
    "diagnostics": [
      {
        "file": "Module.bsl",
        "line": 42,
        "code": "LineLength",
        "severity": "warning",
        "message": "Длина строки превышает 120 символов"
      }
    ]
  }
}
```

**Response (Error)**:
```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "status": "error",
  "error": "Path outside mounted root",
  "details": {
    "path": "C:\\Invalid",
    "mountRoot": "D:\\Projects\\1C"
  }
}
```

#### POST `/api/format`

Format BSL source code.

**Request**:
```http
POST /api/format HTTP/1.1
Content-Type: application/json

{
  "src": "D:\\Projects\\MyProject\\Module.bsl",
  "inPlace": true
}
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "status": "success",
  "data": {
    "formatted": true,
    "filesChanged": 1
  }
}
```

#### POST `/api/session/start`

Start a persistent LSP session.

**Request**:
```http
POST /api/session/start HTTP/1.1
Content-Type: application/json

{
  "projectPath": "D:\\Projects\\MyProject"
}
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "status": "success",
  "sessionId": "123e4567-e89b-12d3-a456-426614174000",
  "project": "/workspaces/MyProject"
}
```

#### GET `/api/session/status`

Get session status.

**Request**:
```http
GET /api/session/status?sessionId=123e4567-e89b-12d3-a456-426614174000 HTTP/1.1
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "status": "ready",
  "uptimeSeconds": 120
}
```

#### POST `/api/session/stop`

Stop a session.

**Request**:
```http
POST /api/session/stop?sessionId=123e4567-e89b-12d3-a456-426614174000 HTTP/1.1
```

**Response**:
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "status": "success",
  "stopped": true
}
```

### Usage

**Start Server**:
```bash
set TRANSPORT_MODE=http
java -jar build\libs\mcp-bsl-server-0.1.0-SNAPSHOT.jar
```

Server will listen on `http://localhost:8080`.

**Test with curl**:
```bash
curl -X POST http://localhost:8080/api/analyze ^
  -H "Content-Type: application/json" ^
  -d "{\"srcDir\":\"D:\\\\Projects\\\\Test\",\"reporters\":[\"json\"]}"
```

**Test with PowerShell**:
```powershell
$body = @{
    srcDir = "D:\Projects\Test"
    reporters = @("json")
    language = "ru"
} | ConvertTo-Json

Invoke-RestMethod -Uri http://localhost:8080/api/analyze `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

### Swagger UI

Browse and test API interactively:

**URL**: `http://localhost:8080/swagger-ui/index.html`

Features:
- Interactive API documentation
- Try-out each endpoint
- View request/response schemas
- Authentication testing (when enabled)

## 3. SSE (Server-Sent Events)

**Status**: ✅ Implemented

**Purpose**: Stream long-running analysis results in real-time.

### Endpoint

```http
POST /api/stream/analyze/sse HTTP/1.1
Content-Type: application/json
Accept: text/event-stream

{
  "srcDir": "D:\\Projects\\Large",
  "reporters": ["console"],
  "language": "ru"
}
```

### Expected Response

```
event: start
data: {"status":"started","totalFiles":150}

event: diagnostic
data: {"file":"Module1.bsl","line":42,"severity":"warning","message":"..."}

event: diagnostic
data: {"file":"Module2.bsl","line":15,"severity":"error","message":"..."}

event: summary
data: {"errors":5,"warnings":12,"filesProcessed":150}

event: done
data: {"status":"completed"}
```

### Client Example (JavaScript)

```javascript
// Note: SSE requires POST request, so EventSource won't work directly
// Use fetch with ReadableStream instead:

async function streamAnalysis(srcDir) {
  const response = await fetch('/api/stream/analyze/sse', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream'
    },
    body: JSON.stringify({
      srcDir: srcDir,
      reporters: ['console'],
      language: 'ru'
    })
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    const chunk = decoder.decode(value);
    const lines = chunk.split('\n');
    
    for (const line of lines) {
      if (line.startsWith('event: ')) {
        const eventType = line.substring(7);
      } else if (line.startsWith('data: ')) {
        const data = JSON.parse(line.substring(6));
        console.log(`${eventType}:`, data);
      }
    }
  }
}
```

## 4. NDJSON (Newline Delimited JSON)

**Status**: ✅ Implemented

**Purpose**: Chunked responses over HTTP when SSE not available.

### Usage

```http
POST /api/stream/analyze/ndjson HTTP/1.1
Content-Type: application/json
Accept: application/x-ndjson

{
  "srcDir": "D:\\Projects\\Large",
  "reporters": ["console"],
  "language": "ru"
}
```

### Expected Response

```http
HTTP/1.1 200 OK
Content-Type: application/x-ndjson
Transfer-Encoding: chunked

{"event":"start","data":{"totalFiles":150}}
{"event":"diagnostic","data":{"file":"Module1.bsl","line":42,"severity":"warning"}}
{"event":"diagnostic","data":{"file":"Module2.bsl","line":15,"severity":"error"}}
{"event":"summary","data":{"errors":5,"warnings":12}}
{"event":"done","data":{"status":"completed"}}
```

Each line is a complete JSON object. Client reads line-by-line.

### Client Example (PowerShell)

```powershell
$response = Invoke-WebRequest -Uri http://localhost:8080/api/stream/analyze/ndjson `
    -Method Post `
    -ContentType "application/json" `
    -Headers @{"Accept"="application/x-ndjson"} `
    -Body '{"srcDir":"D:\\Projects\\Large","reporters":["console"],"language":"ru"}'

$reader = [System.IO.StreamReader]::new($response.RawContentStream)
while (!$reader.EndOfStream) {
    $line = $reader.ReadLine()
    if ($line) {
        $obj = $line | ConvertFrom-Json
        Write-Host "$($obj.event): $($obj.data)"
    }
}
```

## Transport Selection

### Decision Matrix

| Requirement | Recommended Transport |
|-------------|----------------------|
| Cursor IDE integration | **stdio** |
| Web UI / dashboard | **HTTP REST** |
| Real-time progress updates | **SSE** |
| Large dataset streaming | **NDJSON** |
| Simple scripting / automation | **HTTP REST** |
| Low latency for single requests | **HTTP REST** |

### Environment Variables

Control transport mode via environment:

```bash
# STDIO mode (default for MCP clients)
set TRANSPORT_MODE=stdio

# HTTP mode (default for standalone server)
set TRANSPORT_MODE=http
set SERVER_PORT=8080

# SSE/NDJSON support
set TRANSPORT_MODE=http
set STREAM_PROTOCOL=sse  # or ndjson
```

## Performance Considerations

### STDIO
- ✅ Lowest overhead
- ✅ Direct process communication
- ❌ Single client only
- ❌ No concurrent requests

### HTTP REST
- ✅ Multiple concurrent clients
- ✅ Standard tooling (curl, browsers)
- ✅ Load balancing / proxying
- ❌ Higher latency than stdio

### SSE
- ✅ Real-time updates
- ✅ Native browser support
- ✅ Automatic reconnection
- ❌ Server holds connection open

### NDJSON
- ✅ Works with any HTTP client
- ✅ Chunked transfer encoding
- ❌ Manual line parsing required
- ❌ No automatic reconnection

## Security

### STDIO
- ✅ No network exposure
- ✅ Process-level isolation
- ✅ No auth needed

### HTTP/SSE/NDJSON
- ⚠️ Network exposed
- ⚠️ Currently no authentication
- ⚠️ Use localhost only or secure with reverse proxy
- 🔒 Future: API key authentication

**Production Recommendations**:
1. Use reverse proxy (nginx, Caddy) with TLS
2. Enable authentication (API keys, OAuth)
3. Restrict CORS origins
4. Rate limiting

## Error Handling

All transports follow consistent error format:

### Success Response
```json
{
  "status": "success",
  "data": { ... }
}
```

### Error Response
```json
{
  "status": "error",
  "error": "Human-readable message",
  "code": "ERROR_CODE",
  "details": { ... }
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `PATH_OUTSIDE_ROOT` | 400 | Path not under mounted root |
| `PATH_NOT_FOUND` | 404 | File or directory not found |
| `BSL_LS_NOT_FOUND` | 503 | BSL Language Server JAR missing |
| `BSL_LS_FAILED` | 500 | BSL LS execution error |
| `SESSION_NOT_FOUND` | 404 | Session ID invalid |
| `INVALID_PARAMS` | 400 | Invalid request parameters |

## See Also

- [Cursor Integration](CURSOR_INTEGRATION.md) - Cursor IDE setup
- [README](../README.md) - Main documentation
- [MCP Protocol](https://modelcontextprotocol.io/) - MCP spec
