# 🎯 Руководство по настройке MCP BSL Server в Cursor IDE

## 📋 Обзор

Это руководство поможет вам правильно настроить MCP BSL Server в Cursor IDE после исправления критических проблем с MCP протоколом.

## ✅ Что исправлено

- **MCP Initialize** - теперь работает корректно
- **MCP Tools List** - Cursor IDE видит все 5 доступных инструментов
- **MCP Tools Call** - запросы analyze выполняются без ошибок
- **JSON-RPC 2.0** - правильная структура всех ответов

## 🚀 Быстрая настройка

### Шаг 1: Подготовка Docker образа

```bash
# Перейдите в папку проекта
cd "D:\My Projects\FrameWork 1C\1c-syntax-checker"

# Соберите Docker образ
docker build -t mcp-bsl-server:latest .
```

### Шаг 2: Запуск контейнера

```bash
# Запустите контейнер с правильными настройками
docker run --rm -d \
  --name mcp-bsl-server-checker \
  -e MCP_TRANSPORT=http \
  -p 9090:9090 \
  -p 8080:8080 \
  -v "D:\My Projects\Projects 1C:/workspaces:ro" \
  mcp-bsl-server:latest
```

### Шаг 3: Настройка Cursor IDE

1. **Скопируйте конфигурацию**:
   - Файл: `cursor-mcp-config.json`
   - Путь: `%APPDATA%\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json`

2. **Обновите путь к проектам** в конфигурации:
   ```json
   "D:\\My Projects\\Projects 1C:/workspaces:ro"
   ```

3. **Включите stdio режим**:
   ```json
   "bsl-mcp-stdio": {
     "disabled": false
   }
   ```

4. **Перезапустите Cursor IDE**

## 🔧 Детальная конфигурация

### Конфигурация для stdio режима (рекомендуется)

```json
{
  "$schema": "https://modelcontextprotocol.io/schema/mcp-config.json",
  "mcpServers": {
    "bsl-mcp-stdio": {
      "description": "🎯 MCP stdio режим для Cursor IDE",
      "command": "docker",
      "args": [
        "exec",
        "-i",
        "mcp-bsl-server-checker",
        "java",
        "-jar",
        "/app/app.jar"
      ],
      "env": {
        "MCP_TRANSPORT": "stdio",
        "LOGGING_ENABLED": "false"
      },
      "disabled": false,
      "alwaysAllow": [
        "bslcheck_analyze",
        "bslcheck_format", 
        "bslcheck_session_start",
        "bslcheck_session_status",
        "bslcheck_session_stop"
      ]
    }
  }
}
```

### Конфигурация для HTTP режима (для тестирования)

```json
{
  "$schema": "https://modelcontextprotocol.io/schema/mcp-config.json",
  "mcpServers": {
    "bsl-mcp-http": {
      "description": "🌐 MCP HTTP режим для внешних интеграций",
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
        "LOGGING_ENABLED": "true"
      },
      "disabled": true
    }
  }
}
```

## 🧪 Тестирование подключения

### Автоматический тест

```powershell
# Запустите тестовый скрипт
.\test-mcp-fix.ps1
```

### Ручное тестирование

1. **Проверьте статус контейнера**:
   ```bash
   docker ps --filter name=mcp-bsl-server-checker
   ```

2. **Проверьте Web UI**:
   - Откройте: http://localhost:9090
   - Swagger UI: http://localhost:9090/swagger-ui

3. **Тест MCP Initialize**:
   ```bash
   curl -X POST http://localhost:9090/mcp \
     -H "Content-Type: application/json" \
     -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}}}}'
   ```

4. **Тест MCP Tools List**:
   ```bash
   curl -X POST http://localhost:9090/mcp \
     -H "Content-Type: application/json" \
     -d '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'
   ```

## 🎯 Доступные инструменты

После успешной настройки в Cursor IDE будут доступны:

1. **`bslcheck_analyze`** - Анализ кода 1C на ошибки и предупреждения
   - Параметры: `srcDir`, `reporters`, `language`
   - Пример: Анализ проекта на русском языке

2. **`bslcheck_format`** - Форматирование кода 1C
   - Параметры: `src`, `inPlace`
   - Пример: Форматирование файла или директории

3. **`bslcheck_session_start`** - Запуск сессии BSL Language Server
   - Параметры: `projectPath`
   - Пример: Запуск сессии для проекта

4. **`bslcheck_session_status`** - Получение статуса сессии
   - Параметры: `sessionId`
   - Пример: Проверка статуса активной сессии

5. **`bslcheck_session_stop`** - Остановка сессии
   - Параметры: `sessionId`
   - Пример: Остановка активной сессии

## 🔍 Устранение неполадок

### Cursor IDE не видит инструменты

1. **Проверьте контейнер**:
   ```bash
   docker ps --filter name=mcp-bsl-server-checker
   ```

2. **Проверьте логи**:
   ```bash
   docker logs mcp-bsl-server-checker
   ```

3. **Перезапустите Cursor IDE**

4. **Проверьте конфигурацию MCP** в Cursor

### Запрос analyze не работает

1. **Проверьте путь к проектам** в volume mapping
2. **Убедитесь, что BSL Language Server доступен** в контейнере
3. **Проверьте права доступа** к файлам проекта

### Web UI недоступен

1. **Проверьте, что порт 9090 не занят**
2. **Убедитесь, что контейнер запущен** с правильными портами
3. **Проверьте настройки firewall**

## 📊 Мониторинг

### Web UI (http://localhost:9090)
- **Swagger UI**: http://localhost:9090/swagger-ui
- **Health Check**: http://localhost:9090/actuator/health
- **Metrics**: http://localhost:9090/actuator/metrics

### Логи
```bash
# Просмотр логов в реальном времени
docker logs -f mcp-bsl-server-checker

# Последние 50 строк
docker logs --tail 50 mcp-bsl-server-checker
```

## 🎉 Результат

После успешной настройки:

- ✅ **Cursor IDE видит все 5 инструментов**
- ✅ **Запросы analyze выполняются корректно**
- ✅ **Web UI доступен для мониторинга**
- ✅ **MCP протокол работает по стандарту JSON-RPC 2.0**

## 📞 Поддержка

Если возникли проблемы:

1. Проверьте логи контейнера
2. Убедитесь, что все шаги выполнены правильно
3. Проверьте, что Docker образ собран с последними исправлениями
4. Перезапустите контейнер и Cursor IDE

---

**Версия**: v0.1.0-fixed  
**Дата**: 2025-10-25  
**Статус**: ✅ Все исправления протестированы и работают
