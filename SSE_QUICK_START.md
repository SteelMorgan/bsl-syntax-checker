# 📡 SSE Quick Start для MCP BSL Server

## ⚡ Автоматическая настройка SSE

```powershell
# Запустите скрипт автоматической настройки SSE
.\setup-sse-mcp.ps1

# С тестированием SSE endpoints
.\setup-sse-mcp.ps1 -TestSSE
```

## 🔧 Ручная настройка SSE

### 1. Сборка и запуск

```bash
# Соберите Docker образ
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

### 2. Настройка Cursor IDE

Скопируйте содержимое файла `cursor-mcp-sse-config.json` в:
```
%APPDATA%\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json
```

### 3. Перезапустите Cursor IDE

## 🧪 Тестирование SSE

### Тест SSE Stream
```bash
curl -X POST http://localhost:9090/api/stream/analyze/sse \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{"srcDir":"D:\\My Projects\\Projects 1C\\MyProject","reporters":["console"],"language":"ru"}'
```

### Тест NDJSON Stream
```bash
curl -X POST http://localhost:9090/api/stream/analyze/ndjson \
  -H "Content-Type: application/json" \
  -H "Accept: application/x-ndjson" \
  -d '{"srcDir":"D:\\My Projects\\Projects 1C\\MyProject","reporters":["console"],"language":"ru"}'
```

## 🌐 Web UI

- **Web UI**: http://localhost:9090
- **Swagger UI**: http://localhost:9090/swagger-ui
- **SSE Endpoint**: http://localhost:9090/api/stream/analyze/sse
- **NDJSON Endpoint**: http://localhost:9090/api/stream/analyze/ndjson

## 🎯 Преимущества SSE

- ✅ **Real-time анализ** - диагностика в процессе обработки
- ✅ **Потоковые результаты** - не ждете завершения всего анализа
- ✅ **Прогресс мониторинг** - видите количество обработанных файлов
- ✅ **Мгновенная обратная связь** - ошибки показываются сразу

## 🔍 Устранение неполадок

```bash
# Проверка контейнера
docker ps --filter name=mcp-bsl-server-checker

# Просмотр логов
docker logs mcp-bsl-server-checker

# Остановка
docker stop mcp-bsl-server-checker
```

---

**Статус**: ✅ SSE транспорт реализован и готов к использованию  
**Версия**: v0.1.0-sse
