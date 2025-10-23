# Режимы работы MCP BSL Server

## 📋 Текущая архитектура

MCP BSL Server поддерживает **одновременно несколько режимов работы**:

### Что работает ВСЕГДА:

1. **Web UI** (порт 9090):
   - Swagger UI
   - Health Check
   - Prometheus метрики
   - Страница статуса

2. **HTTP REST API** (порт 8080):
   - `/api/analyze` - анализ кода
   - `/api/format` - форматирование
   - `/api/session/*` - управление сессиями

### Что переключается:

**Только stdio режим** управляется через переменную окружения `MCP_TRANSPORT`.

---

## 🔧 Как работает переключение

### Переменная окружения `MCP_TRANSPORT`

Устанавливается при **запуске контейнера** (не при сборке!):

```powershell
# Режим stdio (для Cursor IDE)
docker run -e MCP_TRANSPORT=stdio ...

# Режим http (по умолчанию)
docker run -e MCP_TRANSPORT=http ...
```

### Что происходит внутри:

1. **Web UI + HTTP API** — **ВСЕГДА активны** (порты 9090 и 8080)
2. **stdio транспорт** — активируется только если `MCP_TRANSPORT=stdio`

---

## 📊 Режимы работы

### Режим 1: HTTP (по умолчанию)

```powershell
docker run --rm -d \
  --name mcp-bsl-server-checker \
  -e MCP_TRANSPORT=http \
  -p 9090:9090 \
  -p 8080:8080 \
  mcp-bsl-server:latest
```

**Что работает:**
- ✅ Web UI на порту 9090
- ✅ HTTP REST API на порту 8080
- ❌ stdio транспорт НЕ активен

**Использование:**
- Прямые HTTP запросы к API
- Тестирование через Swagger
- Интеграция с внешними системами

---

### Режим 2: stdio (для Cursor IDE)

```powershell
docker run --rm -d \
  --name mcp-bsl-server-checker \
  -e MCP_TRANSPORT=stdio \
  -p 9090:9090 \
  mcp-bsl-server:latest
```

**Что работает:**
- ✅ Web UI на порту 9090
- ✅ HTTP REST API на порту 8080
- ✅ stdio транспорт (для MCP клиентов)

**Использование:**
- Cursor IDE через `docker exec -i`
- Другие MCP-совместимые клиенты

---

### ⚠️ ВАЖНО: Cursor подключается по-другому!

Для Cursor IDE используется **уже запущенный контейнер** с режимом `http`:

1. **Запуск контейнера** (один раз):
   ```powershell
   docker run --rm -d \
     --name mcp-bsl-server-checker \
     -e MCP_TRANSPORT=http \
     -p 9090:9090 \
     -p 8080:8080 \
     mcp-bsl-server:latest
   ```

2. **Cursor подключается через `docker exec`**:
   ```json
   {
     "command": "docker",
     "args": ["exec", "-i", "mcp-bsl-server-checker", "java", "-jar", "/app/app.jar"],
     "env": {
       "MCP_TRANSPORT": "stdio"
     }
   }
   ```

**Как это работает:**
- `docker exec` запускает **новый процесс внутри контейнера**
- Этот процесс получает `MCP_TRANSPORT=stdio` из конфигурации Cursor
- Основной контейнер продолжает работать в режиме `http`

---

## 🎯 Рекомендуемая схема использования

### Для пользователей Cursor:

1. **Запустите контейнер в режиме http** (один раз):
   ```powershell
   docker run --rm -d \
     --name mcp-bsl-server-checker \
     -e MCP_TRANSPORT=http \
     -p 9090:9090 \
     -p 8080:8080 \
     -v "D:\My Projects\Projects 1C:/workspaces:ro" \
     mcp-bsl-server:latest
   ```

2. **Настройте Cursor** (конфигурация со страницы http://localhost:9090):
   - Cursor будет подключаться через `docker exec`
   - Каждое подключение запускает stdio процесс
   - Web UI остаётся доступным для мониторинга

3. **Преимущества:**
   - ✅ Web UI всегда доступен (Swagger, метрики)
   - ✅ Можно делать HTTP запросы параллельно с Cursor
   - ✅ Логи и метрики собираются централизованно

---

## 🔍 Как проверить текущий режим

### 1. Проверить переменную окружения контейнера:
```powershell
docker inspect mcp-bsl-server-checker | ConvertFrom-Json | Select-Object -ExpandProperty Config | Select-Object -ExpandProperty Env | Select-String "MCP_TRANSPORT"
```

### 2. Проверить через API:
```powershell
Invoke-RestMethod http://localhost:9090/status | ConvertTo-Json
```

Ответ:
```json
{
  "mcp": {
    "transport": "http",
    "port": 8080
  }
}
```

---

## 📝 Планы на будущее

### SSE и NDJSON режимы (в разработке)

```powershell
# SSE (Server-Sent Events) - для стриминга
docker run -e MCP_TRANSPORT=sse ...

# NDJSON (Newline-Delimited JSON) - для потоковой обработки
docker run -e MCP_TRANSPORT=ndjson ...
```

**Текущий статус:** Контроллеры реализованы, но полная интеграция в процессе.

---

## ❓ Часто задаваемые вопросы

### В: Можно ли использовать все режимы одновременно?
**О:** Частично ДА:
- HTTP API + Web UI работают ВСЕГДА (независимо от `MCP_TRANSPORT`)
- stdio активируется дополнительно если `MCP_TRANSPORT=stdio`
- Для Cursor используется `docker exec` с отдельным stdio процессом

### В: Нужно ли пересобирать образ для смены режима?
**О:** НЕТ! Режим устанавливается при **запуске контейнера** через `-e MCP_TRANSPORT=...`

### В: Какой режим использовать для Cursor?
**О:** Запустите контейнер в режиме `http`, а Cursor подключится через `docker exec`.

### В: Можно ли переключить режим без перезапуска?
**О:** НЕТ. Нужно остановить контейнер и запустить заново с новой переменной.

---

## 🛠️ Примеры команд

### Запуск в режиме HTTP:
```powershell
docker run --rm -d \
  --name mcp-bsl-server-checker \
  -e MCP_TRANSPORT=http \
  -p 9090:9090 \
  -p 8080:8080 \
  -v "D:\My Projects\Projects 1C:/workspaces:ro" \
  mcp-bsl-server:latest
```

### Запуск в режиме stdio:
```powershell
docker run --rm -d \
  --name mcp-bsl-server-checker \
  -e MCP_TRANSPORT=stdio \
  -p 9090:9090 \
  -v "D:\My Projects\Projects 1C:/workspaces:ro" \
  mcp-bsl-server:latest
```

### Смена режима (перезапуск):
```powershell
# Остановить
docker stop mcp-bsl-server-checker

# Запустить в новом режиме
docker run --rm -d \
  --name mcp-bsl-server-checker \
  -e MCP_TRANSPORT=stdio \
  -p 9090:9090 \
  mcp-bsl-server:latest
```

---

**Обновлено:** Октябрь 2024

