# ⚡ Быстрый старт MCP BSL Server

## 🎯 За 3 шага к работе с 1C кодом в Cursor IDE

### Шаг 1: Настройка переменных окружения

```bash
# Скопируйте пример конфигурации
copy mcp-env.example .env

# Отредактируйте .env файл (укажите путь к вашим проектам 1C)
# MOUNT_HOST_ROOT=D:\My Projects\Projects 1C
```

### Шаг 2: Запуск сервера

```powershell
# Запустите MCP сервер
.\start-mcp-server.ps1
```

### Шаг 3: Настройка Cursor IDE

```powershell
# Откройте стартовую страницу в браузере
Start-Process "http://localhost:9090"

# Скопируйте код подключения из стартовой страницы
# Вставьте его в настройки Cursor IDE:
# %APPDATA%\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json

# Перезапустите Cursor IDE
```

## ✅ Готово!

Теперь в Cursor IDE вы можете использовать команды:
- **"Проверь код 1C на ошибки"**
- **"Отформатируй код 1C"**
- **"Найди проблемы в модуле"**

## 🔄 Режимы работы

Режим работы определяется переменной `MCP_TRANSPORT` в файле `.env`:

### STDIO режим (по умолчанию)
```env
MCP_TRANSPORT=stdio
```
- Использует существующий Docker контейнер
- Быстрый запуск
- Подходит для обычной работы

### HTTP REST API режим
```env
MCP_TRANSPORT=http-rest
```
- REST API endpoints
- Автоматический запуск контейнера
- Swagger UI доступен

### SSE Streaming режим
```env
MCP_TRANSPORT=sse
```
- Потоковый анализ в реальном времени
- Server-Sent Events для диагностики
- Автоматический запуск контейнера

### NDJSON Streaming режим
```env
MCP_TRANSPORT=ndjson
```
- Потоковый JSON для больших проектов
- Автоматический запуск контейнера

## 🌐 Полезные ссылки

- **Стартовая страница**: http://localhost:9090 (код подключения для Cursor)
- **Web UI (Swagger)**: http://localhost:9090/swagger-ui
- **Health Check**: http://localhost:9090/actuator/health

## 🛠️ Управление сервером

```powershell
# Просмотр логов
.\start-mcp-server.ps1 -Logs

# Остановка сервера
.\start-mcp-server.ps1 -Stop

# Перезапуск сервера
.\start-mcp-server.ps1 -Restart
```

## 📚 Подробная документация

См. [SIMPLE_SETUP.md](SIMPLE_SETUP.md) для детальной инструкции.