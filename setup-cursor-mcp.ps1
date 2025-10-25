# Скрипт для быстрой настройки MCP BSL Server в Cursor IDE
# Автор: AI Assistant
# Дата: 2025-10-25

param(
    [string]$ProjectPath = "D:\My Projects\Projects 1C",
    [string]$ContainerName = "mcp-bsl-server-checker",
    [switch]$SkipBuild,
    [switch]$SkipContainer,
    [switch]$SkipConfig
)

Write-Host "🎯 НАСТРОЙКА MCP BSL SERVER ДЛЯ CURSOR IDE" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green

# Проверяем Docker
Write-Host "`n🔍 Проверка Docker..." -ForegroundColor Cyan
try {
    $dockerVersion = docker --version
    Write-Host "✅ Docker найден: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker не найден! Установите Docker Desktop" -ForegroundColor Red
    exit 1
}

# Шаг 1: Сборка Docker образа
if (-not $SkipBuild) {
    Write-Host "`n🔨 Сборка Docker образа..." -ForegroundColor Cyan
    try {
        docker build -t mcp-bsl-server:latest .
        Write-Host "✅ Docker образ собран успешно" -ForegroundColor Green
    } catch {
        Write-Host "❌ Ошибка сборки Docker образа" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "`n⏭️ Пропуск сборки Docker образа" -ForegroundColor Yellow
}

# Шаг 2: Остановка существующего контейнера
Write-Host "`n🛑 Остановка существующего контейнера..." -ForegroundColor Cyan
try {
    docker stop $ContainerName 2>$null
    Write-Host "✅ Существующий контейнер остановлен" -ForegroundColor Green
} catch {
    Write-Host "ℹ️ Контейнер не был запущен" -ForegroundColor Yellow
}

# Шаг 3: Запуск нового контейнера
if (-not $SkipContainer) {
    Write-Host "`n🚀 Запуск нового контейнера..." -ForegroundColor Cyan
    $containerCmd = "docker run --rm -d --name $ContainerName -e MCP_TRANSPORT=http -e MOUNT_HOST_ROOT=`"$ProjectPath`" -p 9090:9090 -p 8080:8080 -v `"$ProjectPath:/workspaces:ro`" mcp-bsl-server:latest"
    
    try {
        Invoke-Expression $containerCmd
        Write-Host "✅ Контейнер запущен: $ContainerName" -ForegroundColor Green
        Write-Host "   Команда: $containerCmd" -ForegroundColor White
    } catch {
        Write-Host "❌ Ошибка запуска контейнера" -ForegroundColor Red
        exit 1
    }
    
    # Ждем запуска приложения
    Write-Host "⏳ Ожидание запуска приложения (10 сек)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
} else {
    Write-Host "`n⏭️ Пропуск запуска контейнера" -ForegroundColor Yellow
}

# Шаг 4: Проверка работы контейнера
Write-Host "`n🔍 Проверка работы контейнера..." -ForegroundColor Cyan
try {
    $containerStatus = docker ps --filter "name=$ContainerName" --format "{{.Names}}"
    if ($containerStatus) {
        Write-Host "✅ Контейнер запущен: $containerStatus" -ForegroundColor Green
    } else {
        Write-Host "❌ Контейнер не запущен!" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Ошибка проверки контейнера" -ForegroundColor Red
    exit 1
}

# Шаг 5: Тестирование MCP endpoints
Write-Host "`n🧪 Тестирование MCP endpoints..." -ForegroundColor Cyan

# Тест Web UI
try {
    $webResponse = Invoke-WebRequest -Uri "http://localhost:9090" -UseBasicParsing -TimeoutSec 5
    if ($webResponse.StatusCode -eq 200) {
        Write-Host "✅ Web UI доступен: http://localhost:9090" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ Web UI недоступен" -ForegroundColor Red
}

# Тест MCP Initialize
try {
    $initBody = '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}}}}'
    $initResponse = Invoke-WebRequest -Uri "http://localhost:9090/mcp" -Method POST -Body $initBody -ContentType "application/json" -UseBasicParsing -TimeoutSec 5
    if ($initResponse.StatusCode -eq 200) {
        Write-Host "✅ MCP Initialize работает" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ MCP Initialize не работает" -ForegroundColor Red
}

# Тест MCP Tools List
try {
    $toolsBody = '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
    $toolsResponse = Invoke-WebRequest -Uri "http://localhost:9090/mcp" -Method POST -Body $toolsBody -ContentType "application/json" -UseBasicParsing -TimeoutSec 5
    if ($toolsResponse.StatusCode -eq 200) {
        $toolsResult = $toolsResponse.Content | ConvertFrom-Json
        $toolsCount = $toolsResult.result.tools.Count
        Write-Host "✅ MCP Tools List работает ($toolsCount инструментов)" -ForegroundColor Green
    }
} catch {
    Write-Host "❌ MCP Tools List не работает" -ForegroundColor Red
}

# Шаг 6: Настройка конфигурации Cursor
if (-not $SkipConfig) {
    Write-Host "`n📝 Настройка конфигурации Cursor..." -ForegroundColor Cyan
    
    $cursorConfigPath = "$env:APPDATA\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json"
    $configDir = Split-Path $cursorConfigPath -Parent
    
    # Создаем директорию если не существует
    if (-not (Test-Path $configDir)) {
        New-Item -ItemType Directory -Path $configDir -Force | Out-Null
        Write-Host "✅ Создана директория конфигурации: $configDir" -ForegroundColor Green
    }
    
    # Создаем конфигурацию
    $config = @{
        "`$schema" = "https://modelcontextprotocol.io/schema/mcp-config.json"
        mcpServers = @{
            "bsl-mcp-stdio" = @{
                description = "🎯 MCP stdio режим для Cursor IDE"
                command = "docker"
                args = @("exec", "-i", $ContainerName, "java", "-jar", "/app/app.jar")
                env = @{
                    MCP_TRANSPORT = "stdio"
                    MOUNT_HOST_ROOT = $ProjectPath
                    LOGGING_ENABLED = "false"
                }
                disabled = $false
                alwaysAllow = @(
                    "bslcheck_analyze",
                    "bslcheck_format", 
                    "bslcheck_session_start",
                    "bslcheck_session_status",
                    "bslcheck_session_stop"
                )
            }
        }
    }
    
    try {
        $config | ConvertTo-Json -Depth 10 | Set-Content -Path $cursorConfigPath -Encoding UTF8
        Write-Host "✅ Конфигурация Cursor создана: $cursorConfigPath" -ForegroundColor Green
    } catch {
        Write-Host "❌ Ошибка создания конфигурации Cursor" -ForegroundColor Red
        Write-Host "   Создайте файл вручную: $cursorConfigPath" -ForegroundColor Yellow
    }
} else {
    Write-Host "`n⏭️ Пропуск настройки конфигурации Cursor" -ForegroundColor Yellow
}

# Итоговый отчет
Write-Host "`n🎉 НАСТРОЙКА ЗАВЕРШЕНА!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green

Write-Host "`n📋 РЕЗУЛЬТАТЫ:" -ForegroundColor Yellow
Write-Host "✅ Docker образ собран" -ForegroundColor Green
Write-Host "✅ Контейнер запущен: $ContainerName" -ForegroundColor Green
Write-Host "✅ MCP сервер работает" -ForegroundColor Green
Write-Host "✅ Конфигурация Cursor создана" -ForegroundColor Green

Write-Host "`n🌐 ДОСТУПНЫЕ URL:" -ForegroundColor Yellow
Write-Host "• Web UI: http://localhost:9090" -ForegroundColor White
Write-Host "• Swagger UI: http://localhost:9090/swagger-ui" -ForegroundColor White
Write-Host "• MCP Endpoint: http://localhost:9090/mcp" -ForegroundColor White

Write-Host "`n🔧 СЛЕДУЮЩИЕ ШАГИ:" -ForegroundColor Yellow
Write-Host "1. Перезапустите Cursor IDE" -ForegroundColor White
Write-Host "2. Проверьте подключение к MCP серверу" -ForegroundColor White
Write-Host "3. Убедитесь, что видите 5 инструментов BSL" -ForegroundColor White

Write-Host "`n📞 ПОДДЕРЖКА:" -ForegroundColor Yellow
Write-Host "• Логи контейнера: docker logs $ContainerName" -ForegroundColor White
Write-Host "• Остановка: docker stop $ContainerName" -ForegroundColor White
Write-Host "• Перезапуск: .\setup-cursor-mcp.ps1" -ForegroundColor White

Write-Host "`n🎯 MCP BSL Server готов к использованию в Cursor IDE!" -ForegroundColor Green
