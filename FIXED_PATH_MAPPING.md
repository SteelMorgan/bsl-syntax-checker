# 🔧 Исправление проблемы Path Mapping

## ❌ Проблема
При вызове метода `analyze` возникает ошибка:
```json
{
  "error": "Path mapping is not configured. Set MOUNT_HOST_ROOT environment variable.",
  "code": "INVALID_REQUEST"
}
```

## ✅ Решение

### 1. Остановите текущий контейнер
```bash
docker stop mcp-bsl-server-checker
```

### 2. Запустите контейнер с правильными настройками
```bash
docker run --rm -d \
  --name mcp-bsl-server-checker \
  -e MCP_TRANSPORT=http \
  -e MOUNT_HOST_ROOT="D:\My Projects\Projects 1C" \
  -p 9090:9090 \
  -p 8080:8080 \
  -v "D:\My Projects\Projects 1C:/workspaces:ro" \
  mcp-bsl-server:latest
```

### 3. Обновите конфигурацию Cursor

В файле `cursor-mcp-config.json` добавьте переменную окружения:

```json
{
  "mcpServers": {
    "bsl-mcp-stdio": {
      "env": {
        "MCP_TRANSPORT": "stdio",
        "MOUNT_HOST_ROOT": "D:\\My Projects\\Projects 1C",
        "LOGGING_ENABLED": "false"
      }
    }
  }
}
```

### 4. Используйте правильные пути в запросах

**❌ Неправильно:**
```json
{
  "arguments": {
    "srcDir": "/workspaces"
  }
}
```

**✅ Правильно:**
```json
{
  "arguments": {
    "srcDir": "D:\\My Projects\\Projects 1C"
  }
}
```

## 🧪 Тестирование

### Тест 1: MCP Initialize
```bash
curl -X POST http://localhost:9090/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}}}}'
```

### Тест 2: MCP Tools List
```bash
curl -X POST http://localhost:9090/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
```

### Тест 3: MCP Analyze (с правильным путем)
```bash
curl -X POST http://localhost:9090/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"bslcheck_analyze","arguments":{"srcDir":"D:\\My Projects\\Projects 1C","reporters":["json"],"language":"ru"}}}'
```

## 📋 Обновленная команда запуска

```bash
# Полная команда с правильными настройками
docker run --rm -d \
  --name mcp-bsl-server-checker \
  -e MCP_TRANSPORT=http \
  -e MOUNT_HOST_ROOT="D:\My Projects\Projects 1C" \
  -e LOGGING_ENABLED=true \
  -p 9090:9090 \
  -p 8080:8080 \
  -v "D:\My Projects\Projects 1C:/workspaces:ro" \
  mcp-bsl-server:latest
```

## 🔍 Проверка работы

1. **Проверьте контейнер:**
   ```bash
   docker ps --filter name=mcp-bsl-server-checker
   ```

2. **Проверьте логи:**
   ```bash
   docker logs mcp-bsl-server-checker
   ```

3. **Проверьте Web UI:**
   - http://localhost:9090
   - http://localhost:9090/swagger-ui

## ✅ Результат

После исправления:
- ✅ Path mapping настроен корректно
- ✅ Запросы analyze работают без ошибок
- ✅ Cursor IDE может анализировать проекты 1C
- ✅ Все 5 инструментов BSL доступны

---

**Статус**: ✅ Исправлено  
**Дата**: 2025-10-25
