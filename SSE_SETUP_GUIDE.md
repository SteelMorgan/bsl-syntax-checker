# 📡 Настройка MCP BSL Server с SSE для Cursor IDE

## 🎯 Что такое SSE?

**Server-Sent Events (SSE)** - это технология для потоковой передачи данных в реальном времени от сервера к клиенту. В контексте MCP BSL Server это означает:

- ✅ **Real-time анализ** - диагностика приходит по мере обработки файлов
- ✅ **Потоковые результаты** - не нужно ждать завершения всего анализа
- ✅ **Прогресс в реальном времени** - видите, сколько файлов обработано
- ✅ **Мгновенная обратная связь** - ошибки показываются сразу

## 🚀 Быстрая настройка SSE

### Шаг 1: Подготовка Docker контейнера

```bash
# Соберите Docker образ (если еще не собран)
docker build -t mcp-bsl-server:latest .

# Запустите контейнер с SSE поддержкой
docker run --rm -d \
  --name mcp-bsl-server-checker \
  -e MCP_TRANSPORT=http \
  -e MOUNT_HOST_ROOT="D:\My Projects\Projects 1C" \
  -p 9090:9090 \
  -p 8080:8080 \
  -v "D:\My Projects\Projects 1C:/workspaces:ro" \
  mcp-bsl-server:latest
```

### Шаг 2: Конфигурация Cursor IDE

Создайте файл конфигурации:
**Путь:** `%APPDATA%\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json`

**Содержимое:**
```json
{
  "$schema": "https://modelcontextprotocol.io/schema/mcp-config.json",
  "mcpServers": {
    "bsl-checker": {
      "description": "📡 MCP SSE режим для потокового анализа",
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-d",
        "-p",
        "9090:9090",
        "-p", 
        "8080:8080",
        "-v",
        "D:\\My Projects\\Projects 1C:/workspaces:ro",
        "--name",
        "mcp-bsl-server-checker",
        "mcp-bsl-server:latest"
      ],
      "env": {
        "MCP_TRANSPORT": "http",
        "MCP_PORT": "8080",
        "WEB_UI_PORT": "9090",
        "MOUNT_HOST_ROOT": "D:\\My Projects\\Projects 1C",
        "LOGGING_ENABLED": "true"
      },
      "disabled": false
    }
  }
}
```

### Шаг 3: Перезапуск Cursor IDE

1. Сохраните конфигурацию
2. Полностью закройте Cursor IDE
3. Запустите Cursor IDE заново

## 🧪 Тестирование SSE

### Тест 1: SSE Stream Analysis

```bash
curl -X POST http://localhost:9090/api/stream/analyze/sse \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "srcDir": "D:\\My Projects\\Projects 1C\\MyProject",
    "reporters": ["console"],
    "language": "ru"
  }'
```

**Ожидаемый ответ:**
```
event: start
data: {"srcDir":"/workspaces/MyProject","status":"started"}

event: diagnostic
data: {"file":"Module1.bsl","line":42,"severity":"warning","message":"Неиспользуемая переменная"}

event: diagnostic
data: {"file":"Module2.bsl","line":15,"severity":"error","message":"Синтаксическая ошибка"}

event: summary
data: {"errors":5,"warnings":12,"filesProcessed":150}

event: done
data: {"status":"completed"}
```

### Тест 2: NDJSON Stream Analysis

```bash
curl -X POST http://localhost:9090/api/stream/analyze/ndjson \
  -H "Content-Type: application/json" \
  -H "Accept: application/x-ndjson" \
  -d '{
    "srcDir": "D:\\My Projects\\Projects 1C\\MyProject",
    "reporters": ["console"],
    "language": "ru"
  }'
```

**Ожидаемый ответ:**
```json
{"event":"start","data":{"srcDir":"/workspaces/MyProject","status":"started"}}
{"event":"diagnostic","data":{"file":"Module1.bsl","line":42,"severity":"warning","message":"Неиспользуемая переменная"}}
{"event":"diagnostic","data":{"file":"Module2.bsl","line":15,"severity":"error","message":"Синтаксическая ошибка"}}
{"event":"summary","data":{"errors":5,"warnings":12,"filesProcessed":150}}
{"event":"done","data":{"status":"completed"}}
```

## 🌐 Web UI для мониторинга

### Доступные URL:

- **Web UI**: http://localhost:9090
- **Swagger UI**: http://localhost:9090/swagger-ui
- **SSE Endpoint**: http://localhost:9090/api/stream/analyze/sse
- **NDJSON Endpoint**: http://localhost:9090/api/stream/analyze/ndjson

### Swagger UI тестирование:

1. Откройте http://localhost:9090/swagger-ui
2. Найдите секцию "Streaming"
3. Попробуйте endpoints:
   - `POST /api/stream/analyze/sse`
   - `POST /api/stream/analyze/ndjson`

## 💻 JavaScript клиент для SSE

```javascript
async function streamAnalysis(srcDir) {
  const response = await fetch('http://localhost:9090/api/stream/analyze/sse', {
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
        console.log(`Event: ${eventType}`);
      } else if (line.startsWith('data: ')) {
        const data = JSON.parse(line.substring(6));
        console.log('Data:', data);
      }
    }
  }
}

// Использование
streamAnalysis('D:\\My Projects\\Projects 1C\\MyProject');
```

## 🔧 PowerShell клиент для NDJSON

```powershell
function Stream-Analysis {
    param(
        [string]$SrcDir = "D:\My Projects\Projects 1C\MyProject"
    )
    
    $body = @{
        srcDir = $SrcDir
        reporters = @("console")
        language = "ru"
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri "http://localhost:9090/api/stream/analyze/ndjson" `
        -Method Post `
        -ContentType "application/json" `
        -Headers @{"Accept"="application/x-ndjson"} `
        -Body $body
    
    $reader = [System.IO.StreamReader]::new($response.RawContentStream)
    while (!$reader.EndOfStream) {
        $line = $reader.ReadLine()
        if ($line) {
            $obj = $line | ConvertFrom-Json
            Write-Host "[$($obj.event)] $($obj.data | ConvertTo-Json -Compress)"
        }
    }
}

# Использование
Stream-Analysis
```

## 🎯 Преимущества SSE режима

### ✅ Для больших проектов:
- **Real-time прогресс** - видите обработку файлов в реальном времени
- **Раннее обнаружение ошибок** - не ждете завершения всего анализа
- **Потоковая обработка** - результаты приходят по мере готовности

### ✅ Для разработки:
- **Мгновенная обратная связь** - ошибки показываются сразу
- **Интерактивный анализ** - можете прервать и начать заново
- **Детальная диагностика** - каждая ошибка с контекстом

### ✅ Для CI/CD:
- **NDJSON формат** - легко парсится скриптами
- **Прогресс мониторинг** - можно отслеживать статус
- **Структурированные данные** - JSON формат для автоматизации

## 🔍 Устранение неполадок

### SSE не работает:
```bash
# Проверьте контейнер
docker ps --filter name=mcp-bsl-server-checker

# Проверьте логи
docker logs mcp-bsl-server-checker

# Проверьте Web UI
curl http://localhost:9090/actuator/health
```

### Поток прерывается:
- Увеличьте timeout в клиенте
- Проверьте размер проекта
- Используйте NDJSON для больших проектов

### Cursor IDE не видит инструменты:
- Перезапустите Cursor IDE
- Проверьте конфигурацию MCP
- Убедитесь, что контейнер запущен

## 🎉 Результат

После настройки SSE режима:

- ✅ **Real-time анализ** проектов 1C
- ✅ **Потоковые результаты** в Cursor IDE
- ✅ **Web UI мониторинг** на http://localhost:9090
- ✅ **Swagger UI** для тестирования endpoints
- ✅ **NDJSON поддержка** для автоматизации

---

**Статус**: ✅ SSE транспорт реализован и готов к использованию  
**Версия**: v0.1.0-sse  
**Дата**: 2025-10-25
