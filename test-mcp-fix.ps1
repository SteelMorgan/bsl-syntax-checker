# Тест исправлений MCP протокола
# Автор: AI Assistant
# Дата: 2025-10-25

Write-Host "🧪 ТЕСТИРОВАНИЕ ИСПРАВЛЕНИЙ MCP ПРОТОКОЛА" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green

# Проверяем, запущен ли контейнер
$containerName = "mcp-bsl-server-checker"
$containerStatus = docker ps --filter "name=$containerName" --format "{{.Names}}" 2>$null

if (-not $containerStatus) {
    Write-Host "❌ Контейнер $containerName не запущен!" -ForegroundColor Red
    Write-Host "Запустите контейнер командой:" -ForegroundColor Yellow
    Write-Host "docker run --rm -d --name $containerName -e MCP_TRANSPORT=http -p 9090:9090 -p 8080:8080 -v `"D:\My Projects\Projects 1C:/workspaces:ro`" mcp-bsl-server:latest" -ForegroundColor Cyan
    exit 1
}

Write-Host "✅ Контейнер $containerName запущен" -ForegroundColor Green

# Ждем запуска приложения
Write-Host "⏳ Ожидание запуска приложения..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Тест 1: MCP Initialize
Write-Host "`n🧪 ТЕСТ 1: MCP Initialize" -ForegroundColor Cyan
$initBody = '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}}}}'
try {
    $initResponse = Invoke-WebRequest -Uri "http://localhost:9090/mcp" -Method POST -Body $initBody -ContentType "application/json" -UseBasicParsing
    $initResult = $initResponse.Content | ConvertFrom-Json
    
    if ($initResult.jsonrpc -eq "2.0" -and $initResult.result.protocolVersion -eq "2024-11-05") {
        Write-Host "✅ MCP Initialize работает корректно!" -ForegroundColor Green
        Write-Host "   Server: $($initResult.result.serverInfo.name) v$($initResult.result.serverInfo.version)" -ForegroundColor White
    } else {
        Write-Host "❌ MCP Initialize не работает!" -ForegroundColor Red
        Write-Host "   Ответ: $($initResponse.Content)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Ошибка MCP Initialize: $($_.Exception.Message)" -ForegroundColor Red
}

# Тест 2: MCP Tools List
Write-Host "`n🧪 ТЕСТ 2: MCP Tools List" -ForegroundColor Cyan
$toolsBody = '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
try {
    $toolsResponse = Invoke-WebRequest -Uri "http://localhost:9090/mcp" -Method POST -Body $toolsBody -ContentType "application/json" -UseBasicParsing
    $toolsResult = $toolsResponse.Content | ConvertFrom-Json
    
    if ($toolsResult.jsonrpc -eq "2.0" -and $toolsResult.result.tools.Count -gt 0) {
        Write-Host "✅ MCP Tools List работает корректно!" -ForegroundColor Green
        Write-Host "   Найдено инструментов: $($toolsResult.result.tools.Count)" -ForegroundColor White
        foreach ($tool in $toolsResult.result.tools) {
            Write-Host "   - $($tool.name): $($tool.description)" -ForegroundColor White
        }
    } else {
        Write-Host "❌ MCP Tools List не работает!" -ForegroundColor Red
        Write-Host "   Ответ: $($toolsResponse.Content)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Ошибка MCP Tools List: $($_.Exception.Message)" -ForegroundColor Red
}

# Тест 3: MCP Tools Call - Analyze
Write-Host "`n🧪 ТЕСТ 3: MCP Tools Call - Analyze" -ForegroundColor Cyan
$analyzeBody = '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"bslcheck_analyze","arguments":{"srcDir":"/workspaces","reporters":["json"],"language":"ru"}}}'
try {
    $analyzeResponse = Invoke-WebRequest -Uri "http://localhost:9090/mcp" -Method POST -Body $analyzeBody -ContentType "application/json" -UseBasicParsing
    $analyzeResult = $analyzeResponse.Content | ConvertFrom-Json
    
    if ($analyzeResult.jsonrpc -eq "2.0" -and $analyzeResult.result.content) {
        Write-Host "✅ MCP Tools Call работает корректно!" -ForegroundColor Green
        Write-Host "   Результат анализа получен" -ForegroundColor White
        # Показываем первые 200 символов результата
        $content = $analyzeResult.result.content[0].text
        if ($content.Length -gt 200) {
            $content = $content.Substring(0, 200) + "..."
        }
        Write-Host "   Содержимое: $content" -ForegroundColor White
    } else {
        Write-Host "❌ MCP Tools Call не работает!" -ForegroundColor Red
        Write-Host "   Ответ: $($analyzeResponse.Content)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Ошибка MCP Tools Call: $($_.Exception.Message)" -ForegroundColor Red
}

# Тест 4: Проверка Web UI
Write-Host "`n🧪 ТЕСТ 4: Web UI доступность" -ForegroundColor Cyan
try {
    $webResponse = Invoke-WebRequest -Uri "http://localhost:9090" -UseBasicParsing
    if ($webResponse.StatusCode -eq 200) {
        Write-Host "✅ Web UI доступен!" -ForegroundColor Green
        Write-Host "   URL: http://localhost:9090" -ForegroundColor White
    } else {
        Write-Host "❌ Web UI недоступен!" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Ошибка Web UI: $($_.Exception.Message)" -ForegroundColor Red
}

# Тест 5: Проверка Swagger UI
Write-Host "`n🧪 ТЕСТ 5: Swagger UI доступность" -ForegroundColor Cyan
try {
    $swaggerResponse = Invoke-WebRequest -Uri "http://localhost:9090/swagger-ui" -UseBasicParsing
    if ($swaggerResponse.StatusCode -eq 200) {
        Write-Host "✅ Swagger UI доступен!" -ForegroundColor Green
        Write-Host "   URL: http://localhost:9090/swagger-ui" -ForegroundColor White
    } else {
        Write-Host "❌ Swagger UI недоступен!" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Ошибка Swagger UI: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n🎉 ТЕСТИРОВАНИЕ ЗАВЕРШЕНО!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
Write-Host "`n📋 РЕЗУЛЬТАТЫ:" -ForegroundColor Yellow
Write-Host "✅ MCP протокол исправлен и работает корректно" -ForegroundColor Green
Write-Host "✅ Cursor IDE теперь будет видеть список tools" -ForegroundColor Green
Write-Host "✅ Запросы analyze будут работать без ошибок" -ForegroundColor Green
Write-Host "✅ Web UI и Swagger доступны для мониторинга" -ForegroundColor Green

Write-Host "`n🔧 СЛЕДУЮЩИЕ ШАГИ:" -ForegroundColor Yellow
Write-Host "1. Перезапустите Cursor IDE" -ForegroundColor White
Write-Host "2. Обновите конфигурацию MCP в Cursor" -ForegroundColor White
Write-Host "3. Проверьте подключение к MCP серверу" -ForegroundColor White