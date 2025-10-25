# PowerShell script to start MCP BSL Server with environment variables
# Usage: .\start-mcp-server.ps1

param(
    [string]$EnvFile = ".env",
    [switch]$Stop,
    [switch]$Restart,
    [switch]$Logs
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
$imageName = if ($env:MCP_IMAGE_NAME) { $env:MCP_IMAGE_NAME } else { "mcp-bsl-server:latest" }
$transport = if ($env:MCP_TRANSPORT) { $env:MCP_TRANSPORT } else { "stdio" }
$webUIPort = if ($env:WEB_UI_PORT) { $env:WEB_UI_PORT } else { "9090" }
$mcpPort = if ($env:MCP_PORT) { $env:MCP_PORT } else { "8080" }
$mountHostRoot = if ($env:MOUNT_HOST_ROOT) { $env:MOUNT_HOST_ROOT } else { "D:\My Projects\Projects 1C" }
$mountContainerRoot = if ($env:MOUNT_CONTAINER_ROOT) { $env:MOUNT_CONTAINER_ROOT } else { "/workspaces" }
$loggingEnabled = if ($env:LOGGING_ENABLED) { $env:LOGGING_ENABLED } else { "false" }
$bslMaxHeap = if ($env:BSL_MAX_HEAP) { $env:BSL_MAX_HEAP } else { "4g" }
$bslPoolMaxSize = if ($env:BSL_POOL_MAX_SIZE) { $env:BSL_POOL_MAX_SIZE } else { "5" }
$bslPoolTtl = if ($env:BSL_POOL_TTL) { $env:BSL_POOL_TTL } else { "60" }

Write-Host "MCP BSL Server Management" -ForegroundColor Cyan
Write-Host "========================" -ForegroundColor Cyan

if ($Stop -or $Restart) {
    Write-Host "Stopping container '$containerName'..." -ForegroundColor Yellow
    docker stop $containerName 2>$null
    docker rm $containerName 2>$null
    Write-Host "Container stopped and removed." -ForegroundColor Green
}

if ($Logs) {
    Write-Host "Showing logs for container '$containerName'..." -ForegroundColor Yellow
    docker logs -f $containerName
    return
}

if (-not $Stop) {
    Write-Host "Starting MCP BSL Server..." -ForegroundColor Green
    Write-Host "Configuration:" -ForegroundColor Cyan
    Write-Host "  Container Name: $containerName" -ForegroundColor White
    Write-Host "  Image: $imageName" -ForegroundColor White
    Write-Host "  Transport: $transport" -ForegroundColor White
    Write-Host "  Web UI Port: $webUIPort" -ForegroundColor White
    Write-Host "  MCP Port: $mcpPort" -ForegroundColor White
    Write-Host "  Mount: $mountHostRoot -> $mountContainerRoot" -ForegroundColor White
    Write-Host "  Logging: $loggingEnabled" -ForegroundColor White

    # Check if image exists
    $imageExists = docker images -q $imageName
    if (-not $imageExists) {
        Write-Host "`nImage '$imageName' not found. Building..." -ForegroundColor Yellow
        docker build -t $imageName .
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Failed to build image!" -ForegroundColor Red
            return
        }
    }

    # Start container
    $dockerArgs = @(
        "run",
        "--rm",
        "-d",
        "--name", $containerName,
        "-p", "${webUIPort}:9090",
        "-p", "${mcpPort}:8080",
        "-v", "${mountHostRoot}:${mountContainerRoot}",
        "-e", "MCP_TRANSPORT=$transport",
        "-e", "MCP_PORT=$mcpPort",
        "-e", "WEB_UI_PORT=9090",
        "-e", "MOUNT_HOST_ROOT=$mountHostRoot",
        "-e", "LOGGING_ENABLED=$loggingEnabled",
        "-e", "BSL_MAX_HEAP=$bslMaxHeap",
        "-e", "BSL_POOL_MAX_SIZE=$bslPoolMaxSize",
        "-e", "BSL_POOL_TTL=$bslPoolTtl",
        $imageName
    )

    Write-Host "`nStarting container..." -ForegroundColor Yellow
    & docker @dockerArgs

    if ($LASTEXITCODE -eq 0) {
        Write-Host "`n✅ MCP BSL Server started successfully!" -ForegroundColor Green
        Write-Host "`nAccess URLs:" -ForegroundColor Cyan
        Write-Host "  Web UI (Swagger): http://localhost:$webUIPort/swagger-ui" -ForegroundColor White
        Write-Host "  MCP Endpoint: http://localhost:$mcpPort/mcp" -ForegroundColor White
        Write-Host "  Health Check: http://localhost:$webUIPort/actuator/health" -ForegroundColor White
        
        Write-Host "`nContainer Status:" -ForegroundColor Cyan
        docker ps --filter name=$containerName --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
        
        Write-Host "`nTo view logs:" -ForegroundColor Cyan
        Write-Host "  .\start-mcp-server.ps1 -Logs" -ForegroundColor White
        
        Write-Host "`nTo stop server:" -ForegroundColor Cyan
        Write-Host "  .\start-mcp-server.ps1 -Stop" -ForegroundColor White
    } else {
        Write-Host "`n❌ Failed to start container!" -ForegroundColor Red
    }
}
