# PowerShell script to generate MCP configuration from environment variables
# Usage: .\generate-mcp-config.ps1

param(
    [string]$EnvFile = ".env",
    [string]$OutputFile = "cursor-mcp-config.json"
)

# Load environment variables from .env file if it exists
if (Test-Path $EnvFile) {
    Write-Host "Loading environment variables from $EnvFile..." -ForegroundColor Green
    
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match '^([^#][^=]+)=(.*)$') {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            # Remove quotes if present
            $value = $value -replace '^"(.*)"$', '$1'
            $value = $value -replace "^'(.*)'$", '$1'
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
} else {
    Write-Host "Environment file $EnvFile not found. Using system environment variables." -ForegroundColor Yellow
}

# Get environment variables with defaults
$containerName = if ($env:MCP_CONTAINER_NAME) { $env:MCP_CONTAINER_NAME } else { "mcp-bsl-server-checker" }
$transport = if ($env:MCP_TRANSPORT) { $env:MCP_TRANSPORT } else { "stdio" }
$loggingEnabled = if ($env:LOGGING_ENABLED) { $env:LOGGING_ENABLED } else { "false" }
$webUIPort = if ($env:WEB_UI_PORT) { $env:WEB_UI_PORT } else { "9090" }
$mcpPort = if ($env:MCP_PORT) { $env:MCP_PORT } else { "8080" }
$mountHostRoot = if ($env:MOUNT_HOST_ROOT) { $env:MOUNT_HOST_ROOT } else { "D:\My Projects\Projects 1C" }

Write-Host "Configuration:" -ForegroundColor Cyan
Write-Host "  Container Name: $containerName" -ForegroundColor White
Write-Host "  Transport: $transport" -ForegroundColor White
Write-Host "  Web UI Port: $webUIPort" -ForegroundColor White
Write-Host "  MCP Port: $mcpPort" -ForegroundColor White
Write-Host "  Mount Host Root: $mountHostRoot" -ForegroundColor White

# Generate MCP configuration JSON based on transport mode
$mcpServers = @{}

switch ($transport) {
    "stdio" {
        # STDIO mode configuration
        $mcpServers["bsl-checker"] = @{
            "description" = "BSL Language Server for 1C code analysis and formatting (STDIO mode)"
            "command" = "docker"
            "args" = @(
                "exec",
                "-i",
                $containerName,
                "java",
                "-jar",
                "/app/app.jar"
            )
            "env" = @{
                "MCP_TRANSPORT" = "stdio"
                "LOGGING_ENABLED" = "false"
            }
            "disabled" = $false
        }
    }
    "http-rest" {
        # HTTP REST API mode configuration
        $mcpServers["bsl-checker"] = @{
            "description" = "BSL Language Server for 1C code analysis and formatting (HTTP REST API mode)"
            "command" = "docker"
            "args" = @(
                "run",
                "--rm",
                "-d",
                "-p",
                "${webUIPort}:9090",
                "-p",
                "${mcpPort}:8080",
                "-v",
                "${mountHostRoot}:/workspaces",
                "--name",
                $containerName,
                "mcp-bsl-server:latest"
            )
            "env" = @{
                "MCP_TRANSPORT" = "http"
                "MCP_PORT" = $mcpPort
                "WEB_UI_PORT" = "9090"
                "MOUNT_HOST_ROOT" = $mountHostRoot
                "LOGGING_ENABLED" = "true"
            }
            "disabled" = $false
        }
    }
    "sse" {
        # SSE Streaming mode configuration
        $mcpServers["bsl-checker"] = @{
            "description" = "BSL Language Server for 1C code analysis and formatting (SSE Streaming mode)"
            "command" = "docker"
            "args" = @(
                "run",
                "--rm",
                "-d",
                "-p",
                "${webUIPort}:9090",
                "-p",
                "${mcpPort}:8080",
                "-v",
                "${mountHostRoot}:/workspaces",
                "--name",
                $containerName,
                "mcp-bsl-server:latest"
            )
            "env" = @{
                "MCP_TRANSPORT" = "sse"
                "MCP_PORT" = $mcpPort
                "WEB_UI_PORT" = "9090"
                "MOUNT_HOST_ROOT" = $mountHostRoot
                "LOGGING_ENABLED" = "true"
            }
            "disabled" = $false
        }
    }
    "ndjson" {
        # NDJSON Streaming mode configuration
        $mcpServers["bsl-checker"] = @{
            "description" = "BSL Language Server for 1C code analysis and formatting (NDJSON Streaming mode)"
            "command" = "docker"
            "args" = @(
                "run",
                "--rm",
                "-d",
                "-p",
                "${webUIPort}:9090",
                "-p",
                "${mcpPort}:8080",
                "-v",
                "${mountHostRoot}:/workspaces",
                "--name",
                $containerName,
                "mcp-bsl-server:latest"
            )
            "env" = @{
                "MCP_TRANSPORT" = "ndjson"
                "MCP_PORT" = $mcpPort
                "WEB_UI_PORT" = "9090"
                "MOUNT_HOST_ROOT" = $mountHostRoot
                "LOGGING_ENABLED" = "true"
            }
            "disabled" = $false
        }
    }
    default {
        Write-Host "Unknown transport mode: $transport. Using stdio as default." -ForegroundColor Yellow
        $transport = "stdio"
        # Fallback to stdio mode
        $mcpServers["bsl-checker"] = @{
            "description" = "BSL Language Server for 1C code analysis and formatting (STDIO mode)"
            "command" = "docker"
            "args" = @(
                "exec",
                "-i",
                $containerName,
                "java",
                "-jar",
                "/app/app.jar"
            )
            "env" = @{
                "MCP_TRANSPORT" = "stdio"
                "LOGGING_ENABLED" = "false"
            }
            "disabled" = $false
        }
    }
}

$mcpConfig = @{
    "`$schema" = "https://modelcontextprotocol.io/schema/mcp-config.json"
    "mcpServers" = $mcpServers
    "_generated" = @{
        "timestamp" = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
        "transport_mode" = $transport
        "container_name" = $containerName
        "web_ui_url" = "http://localhost:$webUIPort"
        "mcp_endpoint" = "http://localhost:$mcpPort/mcp"
        "mount_host_root" = $mountHostRoot
    }
}

# Convert to JSON with proper formatting
$jsonConfig = $mcpConfig | ConvertTo-Json -Depth 10

# Write to output file
$jsonConfig | Out-File -FilePath $OutputFile -Encoding UTF8

Write-Host "`nMCP configuration generated successfully!" -ForegroundColor Green
Write-Host "Output file: $OutputFile" -ForegroundColor White
Write-Host "Transport mode: $transport" -ForegroundColor White

Write-Host "`nNext steps:" -ForegroundColor Cyan
Write-Host "1. Copy the generated file to Cursor MCP settings:" -ForegroundColor White
Write-Host "   Copy-Item '$OutputFile' '$env:APPDATA\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json'" -ForegroundColor Gray

switch ($transport) {
    "stdio" {
        Write-Host "`n2. For STDIO mode:" -ForegroundColor Yellow
        Write-Host "   - Make sure Docker container '$containerName' is running" -ForegroundColor White
        Write-Host "   - Use: .\start-mcp-server.ps1" -ForegroundColor White
    }
    "http-rest" {
        Write-Host "`n2. For HTTP REST API mode:" -ForegroundColor Yellow
        Write-Host "   - Container will be started automatically by Cursor" -ForegroundColor White
        Write-Host "   - REST API endpoints available" -ForegroundColor White
    }
    "sse" {
        Write-Host "`n2. For SSE Streaming mode:" -ForegroundColor Yellow
        Write-Host "   - Container will be started automatically by Cursor" -ForegroundColor White
        Write-Host "   - Server-Sent Events streaming enabled" -ForegroundColor White
    }
    "ndjson" {
        Write-Host "`n2. For NDJSON Streaming mode:" -ForegroundColor Yellow
        Write-Host "   - Container will be started automatically by Cursor" -ForegroundColor White
        Write-Host "   - NDJSON streaming enabled" -ForegroundColor White
    }
}

Write-Host "`n3. Restart Cursor IDE" -ForegroundColor White
Write-Host "4. Web UI will be available at: http://localhost:$webUIPort" -ForegroundColor White

if ($transport -eq "sse" -or $transport -eq "ndjson") {
    Write-Host "`nStreaming Endpoints:" -ForegroundColor Cyan
    Write-Host "  - Analyze Stream (SSE): http://localhost:$webUIPort/api/stream/analyze/sse" -ForegroundColor White
    Write-Host "  - Analyze Stream (NDJSON): http://localhost:$webUIPort/api/stream/analyze/ndjson" -ForegroundColor White
}

if ($transport -eq "http-rest") {
    Write-Host "`nREST API Endpoints:" -ForegroundColor Cyan
    Write-Host "  - Analyze: http://localhost:$webUIPort/api/analyze" -ForegroundColor White
    Write-Host "  - Format: http://localhost:$webUIPort/api/format" -ForegroundColor White
    Write-Host "  - Swagger UI: http://localhost:$webUIPort/swagger-ui" -ForegroundColor White
}
