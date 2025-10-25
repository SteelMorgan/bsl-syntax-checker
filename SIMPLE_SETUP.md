# 🚀 Упрощенная настройка MCP BSL Server

Этот гайд поможет вам быстро настроить MCP BSL Server для Cursor IDE с минимальными усилиями.

## 📋 Быстрый старт

### 1. Настройка переменных окружения

Скопируйте файл с примером переменных окружения:

```bash
copy mcp-env.example .env
```

Отредактируйте `.env` файл под ваши нужды:

```env
# Основные настройки
MCP_CONTAINER_NAME=mcp-bsl-server-checker
MOUNT_HOST_ROOT=D:\My Projects\Projects 1C
WEB_UI_PORT=9090
MCP_PORT=8080
```

### 2. Запуск сервера

Используйте PowerShell скрипт для запуска:

```powershell
# Запуск сервера
.\start-mcp-server.ps1

# Просмотр логов
.\start-mcp-server.ps1 -Logs

# Остановка сервера
.\start-mcp-server.ps1 -Stop

# Перезапуск сервера
.\start-mcp-server.ps1 -Restart
```

### 3. Получение кода подключения

Откройте стартовую страницу в браузере и скопируйте код подключения:

```powershell
# Откройте стартовую страницу
Start-Process "http://localhost:9090"
```

### 4. Установка конфигурации в Cursor

Скопируйте код подключения из стартовой страницы в настройки Cursor:

```powershell
# Путь к файлу настроек Cursor
$cursorSettingsPath = "$env:APPDATA\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json"

# Создайте файл и вставьте код подключения из стартовой страницы
```

### 5. Перезапуск Cursor IDE

Перезапустите Cursor IDE, чтобы загрузить новую конфигурацию MCP.

## 🔧 Альтернативные способы запуска

### Docker Compose

```powershell
# Запуск с docker-compose
docker-compose -f docker-compose.simple.yml up -d

# Остановка
docker-compose -f docker-compose.simple.yml down
```

### Ручной запуск Docker

```powershell
docker run --rm -d \
  --name mcp-bsl-server-checker \
  -p 9090:9090 -p 8080:8080 \
  -v "D:\My Projects\Projects 1C:/workspaces" \
  -e MCP_TRANSPORT=stdio \
  -e MOUNT_HOST_ROOT="D:\My Projects\Projects 1C" \
  mcp-bsl-server:latest
```

## 📁 Структура файлов

```
├── mcp-env.example          # Пример переменных окружения
├── .env                     # Ваши переменные окружения (создать)
├── start-mcp-server.ps1     # Скрипт запуска сервера
├── generate-mcp-config.ps1  # Генерация конфигурации Cursor
├── cursor-mcp-config.json   # Сгенерированная конфигурация Cursor
├── docker-compose.simple.yml # Упрощенный docker-compose
└── SIMPLE_SETUP.md          # Этот файл
```

## 🌐 Доступные URL

После запуска сервера будут доступны:

- **Стартовая страница**: http://localhost:9090 (код подключения для Cursor)
- **Web UI (Swagger)**: http://localhost:9090/swagger-ui
- **MCP Endpoint**: http://localhost:8080/mcp
- **Health Check**: http://localhost:9090/actuator/health

## 🛠️ Доступные инструменты MCP

- `bslcheck_analyze` - Анализ кода 1C на ошибки и предупреждения
- `bslcheck_format` - Форматирование кода 1C
- `bslcheck_session_start` - Запуск сессии BSL Language Server
- `bslcheck_session_status` - Получение статуса сессии
- `bslcheck_session_stop` - Остановка сессии

## 🔍 Проверка работы

### 1. Проверка контейнера

```powershell
docker ps --filter name=mcp-bsl-server-checker
```

### 2. Проверка логов

```powershell
docker logs mcp-bsl-server-checker
```

### 3. Проверка MCP endpoint

```powershell
curl -X POST http://localhost:8080/mcp \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}}}}'
```

### 4. Проверка в Cursor IDE

Откройте Cursor IDE и попробуйте использовать команды:
- "Проверь код 1C на ошибки"
- "Отформатируй код 1C"

## 🚨 Устранение неполадок

### Контейнер не запускается

1. Проверьте, что Docker запущен
2. Проверьте, что порты 9090 и 8080 свободны
3. Проверьте логи: `docker logs mcp-bsl-server-checker`

### Cursor не видит инструменты

1. Убедитесь, что контейнер запущен
2. Проверьте конфигурацию MCP в Cursor
3. Перезапустите Cursor IDE
4. Проверьте путь к файлу настроек

### Ошибки доступа к файлам

1. Проверьте, что путь `MOUNT_HOST_ROOT` существует
2. Убедитесь, что у Docker есть права доступа к директории
3. Проверьте, что том смонтирован правильно

## 📝 Переменные окружения

| Переменная | Описание | По умолчанию |
|------------|----------|--------------|
| `MCP_CONTAINER_NAME` | Имя Docker контейнера | `mcp-bsl-server-checker` |
| `MOUNT_HOST_ROOT` | Путь к проектам 1C на хосте | `D:\My Projects\Projects 1C` |
| `WEB_UI_PORT` | Порт для Web UI | `9090` |
| `MCP_PORT` | Порт для MCP endpoint | `8080` |
| `MCP_TRANSPORT` | Режим транспорта | `stdio` |
| `LOGGING_ENABLED` | Включить логирование | `false` |

## 🎯 Следующие шаги

1. Настройте переменные окружения в `.env`
2. Запустите сервер с помощью `start-mcp-server.ps1`
3. Откройте стартовую страницу http://localhost:9090
4. Скопируйте код подключения из стартовой страницы
5. Вставьте код в настройки Cursor IDE
6. Перезапустите Cursor и наслаждайтесь работой с 1C кодом!
