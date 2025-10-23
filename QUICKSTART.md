# Quick Start Guide

Быстрый старт MCP BSL Server для использования с Cursor IDE и другими MCP-клиентами.

## Предварительные требования

- **Java 17+** ([Eclipse Temurin](https://adoptium.net/) рекомендуется)
- **Docker** (опционально, для контейнеризации)
- **GitHub Personal Access Token** (опционально, для автоматической загрузки BSL LS)

## 1. Локальный запуск (без Docker)

### Шаг 1: Клонировать и собрать

```powershell
# Склонировать репозиторий (если ещё не сделано)
git clone <your-repo-url> mcp-bsl-server
cd mcp-bsl-server

# Проверить Java
java -version  # должно быть 17+

# Собрать проект
.\gradlew.bat build --no-daemon
```

**Результат**: JAR файл в `build/libs/mcp-bsl-server-0.1.0-SNAPSHOT.jar`

### Шаг 2: Скачать BSL Language Server (опционально)

Скачайте последнюю версию BSL LS с [GitHub Releases](https://github.com/1c-syntax/bsl-language-server/releases).

```powershell
# Пример: скачать v0.24.2
mkdir libs
curl -L -o libs/bsl-language-server.jar `
  https://github.com/1c-syntax/bsl-language-server/releases/download/v0.24.2/bsl-language-server-0.24.2-exec.jar
```

### Шаг 3: Запустить сервер

```powershell
# HTTP режим (порт 8080)
$env:TRANSPORT_MODE="http"
$env:BSL_JAR_PATH="D:\path\to\libs\bsl-language-server.jar"
$env:MOUNT_HOST_ROOT="D:\My Projects\Projects 1C"
java -jar build\libs\mcp-bsl-server-0.1.0-SNAPSHOT.jar

# STDIO режим (для Cursor)
$env:TRANSPORT_MODE="stdio"
$env:BSL_JAR_PATH="D:\path\to\libs\bsl-language-server.jar"
$env:MOUNT_HOST_ROOT="D:\My Projects\Projects 1C"
java -jar build\libs\mcp-bsl-server-0.1.0-SNAPSHOT.jar
```

**Проверка**:
```powershell
# HTTP режим
curl http://localhost:8080/status

# Swagger UI
start http://localhost:8080/swagger-ui/index.html
```

## 2. Docker запуск (рекомендуется)

### Шаг 1: Собрать образ

```powershell
# С GitHub токеном (для автоматической загрузки BSL LS)
$token = Get-Content '.secrets\github_token.txt' -Raw
$token = $token.Trim()
docker build --build-arg GITHUB_TOKEN=$token -t mcp-bsl-server:latest .

# Без токена (использует фиксированную версию v0.24.2)
docker build -t mcp-bsl-server:latest .
```

### Шаг 2: Запустить контейнер

```powershell
# Базовый запуск
docker run --rm `
  -p 8080:8080 `
  -v 'D:\My Projects\Projects 1C:/workspaces:ro' `
  --name mcp-bsl `
  mcp-bsl-server:latest

# С кастомными настройками
docker run --rm `
  -p 18080:8080 `
  -e SERVER_PORT=8080 `
  -e BSL_MAX_HEAP=8g `
  -e LOGGING_ENABLED=true `
  -v 'D:\My Projects\Projects 1C:/workspaces:ro' `
  --name mcp-bsl `
  mcp-bsl-server:latest
```

**Проверка**:
```powershell
curl http://localhost:8080/status
# или
curl http://localhost:18080/status  # если порт изменён
```

### Шаг 3: Запустить с docker-compose (полный стек)

```powershell
# Запустить MCP + Loki + Grafana + Prometheus
docker-compose up -d

# Проверить статус
docker-compose ps

# Доступ к сервисам
# MCP Server: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui/index.html
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
```

**Остановка**:
```powershell
docker-compose down
```

## 3. Интеграция с Cursor IDE

### Шаг 1: Создать конфигурацию MCP

Создайте или отредактируйте файл конфигурации Cursor:

**Путь**: `%APPDATA%\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json`

**Содержимое** (для локального запуска):
```json
{
  "mcpServers": {
    "bsl-language-server": {
      "command": "java",
      "args": [
        "-jar",
        "D:\\path\\to\\mcp-bsl-server\\build\\libs\\mcp-bsl-server-0.1.0-SNAPSHOT.jar"
      ],
      "env": {
        "TRANSPORT_MODE": "stdio",
        "LOGGING_ENABLED": "false",
        "MOUNT_HOST_ROOT": "D:\\My Projects\\Projects 1C",
        "BSL_JAR_PATH": "D:\\path\\to\\bsl-language-server.jar",
        "BSL_MAX_HEAP": "4g"
      },
      "disabled": false,
      "alwaysAllow": []
    }
  }
}
```

### Шаг 2: Перезапустить Cursor

Закройте и откройте Cursor IDE заново.

### Шаг 3: Проверить MCP

Откройте панель MCP в Cursor → должен появиться `bsl-language-server` в списке доступных серверов.

### Шаг 4: Использовать

Попросите Cursor:
> "Analyze my 1C project at D:\Projects\MyERPSystem"

Cursor автоматически вызовет инструмент `analyze` из MCP BSL Server.

## 4. Проверка работоспособности

### REST API (HTTP режим)

```powershell
# Проверка статуса
$status = Invoke-RestMethod -Uri http://localhost:8080/status
$status | ConvertTo-Json

# Анализ (требует реальный проект)
$body = @{
    srcDir = "D:\Projects\MyProject"
    reporters = @("json")
    language = "ru"
} | ConvertTo-Json

Invoke-RestMethod -Uri http://localhost:8080/api/analyze `
    -Method Post `
    -ContentType "application/json" `
    -Body $body

# Форматирование
$body = @{
    src = "D:\Projects\MyProject\Module.bsl"
    inPlace = $true
} | ConvertTo-Json

Invoke-RestMethod -Uri http://localhost:8080/api/format `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

### STDIO режим

```powershell
# Запустить в stdio режиме
$env:TRANSPORT_MODE="stdio"
java -jar build\libs\mcp-bsl-server-0.1.0-SNAPSHOT.jar

# Отправить запрос (в другом терминале или через stdin)
# {"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}
```

## 5. Мониторинг (с docker-compose)

### Grafana

1. Откройте http://localhost:3000
2. Логин: `admin` / Пароль: `admin`
3. Перейдите в **Explore** → выберите **Loki** datasource
4. Запрос: `{app="mcp-bsl-server"}`

### Prometheus

1. Откройте http://localhost:9090
2. Запрос: `up{job="mcp-bsl-server"}`

## Troubleshooting

### Ошибка: "JAVA_HOME is not set"

Решение:
```powershell
# Установить Java 17+
winget install EclipseAdoptium.Temurin.17.JDK

# Проверить
$env:JAVA_HOME
java -version
```

### Ошибка: "Path outside mounted root"

Решение: Убедитесь, что:
1. `MOUNT_HOST_ROOT` настроен правильно
2. Путь в запросе находится внутри `MOUNT_HOST_ROOT`

Пример:
```
MOUNT_HOST_ROOT=D:\Projects\1C
Путь в запросе: D:\Projects\1C\MyProject ✅
Путь в запросе: C:\Windows\System32 ❌
```

### Ошибка: "BSL Language Server JAR not found"

Решение:
1. Скачайте BSL LS с GitHub
2. Укажите путь в `BSL_JAR_PATH`
3. Или соберите Docker образ с `GITHUB_TOKEN`

### Контейнер не запускается

Решение:
```powershell
# Проверить логи
docker logs mcp-bsl

# Проверить порты
netstat -an | findstr 8080

# Пересобрать образ
docker build --no-cache -t mcp-bsl-server:latest .
```

## Дополнительная документация

- [README](README.md) - Полная документация проекта
- [TRANSPORTS](docs/TRANSPORTS.md) - Описание транспортов (stdio, HTTP, SSE, NDJSON)
- [CURSOR_INTEGRATION](docs/CURSOR_INTEGRATION.md) - Интеграция с Cursor IDE
- [PROGRESS](PROGRESS.md) - Статус разработки

## Следующие шаги

1. ✅ Запустить сервер локально или в Docker
2. ✅ Интегрировать с Cursor IDE
3. 🔄 Протестировать на реальном 1C проекте
4. 📊 Настроить мониторинг (Grafana + Loki)
5. 🚀 Развернуть в продакшн (с TLS, auth)

---

**Вопросы?** Откройте Issue в репозитории или обратитесь к [документации](docs/).

