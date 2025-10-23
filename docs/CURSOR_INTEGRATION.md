# Cursor Integration Guide

This guide explains how to integrate MCP BSL Server with Cursor IDE.

## Prerequisites

1. **Java 17+** installed and `JAVA_HOME` configured
2. **MCP BSL Server** built: `.\gradlew.bat build`
3. **Cursor** IDE installed
4. **BSL Language Server JAR** available (optional for full functionality)

## Configuration

### 1. Prepare MCP Config

Cursor reads MCP server configurations from a JSON file. Create or edit your Cursor MCP config:

**Location**:
- Windows: `%APPDATA%\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json`
- Or use the workspace-specific config: `.cursor/mcp-config.json`

**Config Template** (see [`mcp-config.json`](../mcp-config.json)):

```json
{
  "mcpServers": {
    "bsl-language-server": {
      "command": "java",
      "args": [
        "-jar",
        "D:\\Path\\To\\1c-syntax-checker\\build\\libs\\mcp-bsl-server-0.1.0-SNAPSHOT.jar"
      ],
      "env": {
        "TRANSPORT_MODE": "stdio",
        "LOGGING_ENABLED": "false",
        "MOUNT_HOST_ROOT": "D:\\My Projects\\Projects 1C",
        "BSL_JAR_PATH": "D:\\Path\\To\\bsl-language-server.jar",
        "BSL_MAX_HEAP": "4g"
      },
      "disabled": false,
      "alwaysAllow": []
    }
  }
}
```

### 2. Adjust Paths

Update the following paths in the config:

| Field | Description | Example |
|-------|-------------|---------|
| `args[1]` | Path to MCP BSL Server JAR | `D:\\...\\mcp-bsl-server-0.1.0-SNAPSHOT.jar` |
| `MOUNT_HOST_ROOT` | Root directory for 1C projects | `D:\\My Projects\\Projects 1C` |
| `BSL_JAR_PATH` | Path to BSL Language Server JAR | `D:\\Tools\\bsl-language-server.jar` |

**Important**: Use absolute paths with double backslashes (`\\`) on Windows.

### 3. Test Configuration

Restart Cursor and open the MCP panel. You should see `bsl-language-server` in the list of available MCP servers.

## Available MCP Tools

Once integrated, Cursor can use these tools:

### 1. `analyze`

Run BSL Language Server analysis on source code.

**Parameters**:
- `srcDir` (string, required): Host absolute path to source directory
- `reporters` (array, optional): List of reporters, default `["json"]`
- `language` (string, optional): Diagnostics language, `"ru"` or `"en"`, default `"ru"`

**Example**:
```json
{
  "srcDir": "D:\\Projects\\MyProject\\src",
  "reporters": ["json"],
  "language": "ru"
}
```

**Response**:
```json
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
        "message": "Line is too long"
      }
    ]
  }
}
```

### 2. `format`

Format BSL source code.

**Parameters**:
- `src` (string, required): Host absolute path to file or directory
- `inPlace` (boolean, optional): Format in place, default `true`

**Example**:
```json
{
  "src": "D:\\Projects\\MyProject\\Module.bsl",
  "inPlace": true
}
```

**Response**:
```json
{
  "status": "success",
  "data": {
    "formatted": true,
    "filesChanged": 1
  }
}
```

### 3. `session_start`

Start a persistent BSL LSP session.

**Parameters**:
- `projectPath` (string, required): Host absolute path to project directory

**Example**:
```json
{
  "projectPath": "D:\\Projects\\MyProject"
}
```

**Response**:
```json
{
  "status": "success",
  "sessionId": "123e4567-e89b-12d3-a456-426614174000",
  "project": "/workspaces/MyProject"
}
```

### 4. `session_status`

Get status of an LSP session.

**Parameters**:
- `sessionId` (string, required): Session ID from `session_start`

**Response**:
```json
{
  "status": "ready",
  "uptimeSeconds": 120
}
```

### 5. `session_stop`

Stop an LSP session.

**Parameters**:
- `sessionId` (string, required): Session ID to stop

**Response**:
```json
{
  "status": "success",
  "stopped": true
}
```

## Usage Examples in Cursor

### Example 1: Analyze Current Project

Ask Cursor:
> "Analyze my 1C project at D:\Projects\MyERPSystem for code quality issues"

Cursor will use the `analyze` tool and show you diagnostics.

### Example 2: Format Code

Ask Cursor:
> "Format the BSL file D:\Projects\MyERPSystem\Module.bsl"

Cursor will use the `format` tool to reformat the code.

### Example 3: Interactive Session

Ask Cursor:
> "Start an LSP session for D:\Projects\MyERPSystem and analyze it"

Cursor will:
1. Call `session_start` to create a session
2. Use the session for analysis
3. Optionally call `session_stop` when done

## Troubleshooting

### MCP Server Not Starting

**Symptom**: Cursor shows "Failed to start bsl-language-server"

**Solutions**:
1. Verify Java 17+ is installed: `java -version`
2. Check JAR path in config is correct
3. Look at Cursor logs (Help → Show Logs)
4. Test manually:
   ```bash
   set TRANSPORT_MODE=stdio
   java -jar build\libs\mcp-bsl-server-0.1.0-SNAPSHOT.jar
   ```

### Path Mapping Errors

**Symptom**: "Path outside mounted root" errors

**Solutions**:
1. Ensure `MOUNT_HOST_ROOT` is set correctly
2. Verify paths in tool calls are under `MOUNT_HOST_ROOT`
3. Use absolute paths with correct separators (`\\` on Windows)

### BSL LS Not Found

**Symptom**: "BSL Language Server JAR not found"

**Solutions**:
1. Download BSL LS from [GitHub releases](https://github.com/1c-syntax/bsl-language-server/releases)
2. Update `BSL_JAR_PATH` in config
3. For development: MCP server will work without BSL LS (returns mock data)

### Session Timeout

**Symptom**: "Session not found" after some time

**Solutions**:
1. Increase `BSL_POOL_TTL` environment variable (default 60 minutes)
2. Use `session_status` to check if session is still alive
3. Restart session with `session_start`

## Advanced Configuration

### Memory Tuning

For large projects, increase JVM heap:

```json
{
  "env": {
    "BSL_MAX_HEAP": "8g"
  }
}
```

### Session Pool Configuration

Control session pool size and TTL:

```json
{
  "env": {
    "BSL_POOL_MAX_SIZE": "10",
    "BSL_POOL_TTL": "120"
  }
}
```

### Multiple Project Roots

To support multiple 1C project locations, configure multiple MCP servers:

```json
{
  "mcpServers": {
    "bsl-projects-main": {
      "command": "java",
      "args": ["..."],
      "env": {
        "MOUNT_HOST_ROOT": "D:\\Projects\\1C"
      }
    },
    "bsl-projects-test": {
      "command": "java",
      "args": ["..."],
      "env": {
        "MOUNT_HOST_ROOT": "D:\\TestProjects\\1C"
      }
    }
  }
}
```

### Logging for Debugging

Enable logging to troubleshoot issues:

```json
{
  "env": {
    "LOGGING_ENABLED": "true",
    "logging.loki.url": "http://localhost:3100/loki/api/v1/push"
  }
}
```

Then run Loki locally (see [docker-compose.yml](../docker-compose.yml)).

## Performance Tips

1. **Use Sessions**: For multiple operations on same project, create a session once
2. **Limit Scope**: Analyze specific directories instead of entire project
3. **Tune Heap**: Increase `BSL_MAX_HEAP` for large codebases
4. **Pool Size**: Increase `BSL_POOL_MAX_SIZE` if working with many projects

## See Also

- [Transports Guide](TRANSPORTS.md) - Detailed transport documentation
- [README](../README.md) - Main documentation
- [BSL Language Server](https://1c-syntax.github.io/bsl-language-server/en/) - BSL LS docs
- [MCP Protocol](https://modelcontextprotocol.io/) - MCP specification

