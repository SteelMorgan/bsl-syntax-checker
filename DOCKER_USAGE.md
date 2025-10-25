# Docker Usage Guide

## Доступные образы

После сборки Docker образ доступен с двумя тегами:

```
REPOSITORY       TAG       IMAGE ID       SIZE
mcp-bsl-server   latest    651ed3f43777   550MB
mcp-bsl-server   v0.1.0    651ed3f43777   550MB
```

## Быстрый запуск

### 1. Stdio режим (для Cursor IDE)

```powershell
docker run --rm -i `
  --network host `
  -v 'D:\My Projects\Projects 1C:/workspaces' `
  mcp-bsl-server:latest
```

**Доступ**:
- MCP: stdin/stdout (для Cursor)
- Web UI: http://localhost:9090

### 2. HTTP режим (для тестирования/отладки)

```powershell
docker run --rm -d `
  --name mcp-bsl-http `
  -e MCP_TRANSPORT=http `
  -p 9090:9090 `
  -p 8080:8080 `
  -v 'D:\My Projects\Projects 1C:/workspaces' `
  mcp-bsl-server:latest
```

**Доступ**:
- Web UI: http://localhost:9090
- MCP API: http://localhost:9090
- Swagger: http://localhost:9090/swagger-ui

**Управление**:
```powershell
# Просмотр логов
docker logs -f mcp-bsl-http

# Остановка
docker stop mcp-bsl-http
```

### 3. Docker Compose (полный стек с мониторингом)

```powershell
docker-compose up -d
```

**Доступ**:
- MCP Server: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9091

**Управление**:
```powershell
# Остановка
docker-compose down

# Просмотр логов
docker-compose logs -f mcp-bsl-server
```

## Переменные окружения

| Переменная | По умолчанию | Описание |
|------------|--------------|----------|
| `MCP_TRANSPORT` | stdio | Режим транспорта: stdio, http, sse, ndjson |
| `WEB_UI_PORT` | 9090 | Порт Web UI (всегда активен) |
| `MCP_PORT` | 9090 | Порт MCP API (для http/sse/ndjson) |
| `LOGGING_ENABLED` | true | Включить логирование в Loki |
| `BSL_MAX_HEAP` | 4g | Максимальная память JVM для BSL LS |

## Монтирование проектов

**Обязательно** монтируйте директорию с вашими 1C проектами:

```powershell
-v 'D:\My Projects\Projects 1C:/workspaces'
```

Где:
- `D:\My Projects\Projects 1C` - путь на хосте
- `/workspaces` - путь в контейнере (фиксированный)

**Важно**: Том монтируется с правами на запись для поддержки форматирования файлов (`bslcheck_format`).

## Примеры использования

### Запуск с custom портами

```powershell
docker run --rm -d `
  --name mcp-bsl-custom `
  -e MCP_TRANSPORT=http `
  -e WEB_UI_PORT=9090 `
  -e MCP_PORT=8080 `
  -p 9091:9090 `
  -p 8081:8080 `
  -v 'D:\My Projects\Projects 1C:/workspaces' `
  mcp-bsl-server:latest
```

Доступ:
- Web UI: http://localhost:9091
- MCP API: http://localhost:9091

### Запуск без логирования

```powershell
docker run --rm -d `
  --name mcp-bsl-no-logs `
  -e LOGGING_ENABLED=false `
  -p 9090:9090 `
  -v 'D:\My Projects\Projects 1C:/workspaces' `
  mcp-bsl-server:latest
```

### Запуск с увеличенной памятью

```powershell
docker run --rm -d `
  --name mcp-bsl-big `
  -e BSL_MAX_HEAP=8g `
  -p 9090:9090 `
  -v 'D:\My Projects\Projects 1C:/workspaces' `
  mcp-bsl-server:latest
```

## Управление образами

### Просмотр доступных образов

```powershell
docker images mcp-bsl-server
```

### Использование конкретной версии

```powershell
docker run ... mcp-bsl-server:v0.1.0
```

или

```powershell
docker run ... mcp-bsl-server:latest
```

### Удаление образа

```powershell
# Удалить конкретную версию
docker rmi mcp-bsl-server:v0.1.0

# Удалить все версии
docker rmi mcp-bsl-server:latest mcp-bsl-server:v0.1.0
```

### Пересборка образа

```powershell
# Загрузить GitHub token
$env:GITHUB_TOKEN = (Get-Content '.secrets\github_token.txt' -Raw).Trim()

# Пересобрать
docker build --build-arg GITHUB_TOKEN=$env:GITHUB_TOKEN -t mcp-bsl-server:latest -t mcp-bsl-server:v0.1.0 .
```

## Troubleshooting

### Образ не найден

```
Error: No such image: mcp-bsl-server:latest
```

**Решение**: Соберите образ:
```powershell
.\gradlew.bat build
$env:GITHUB_TOKEN = (Get-Content '.secrets\github_token.txt' -Raw).Trim()
docker build --build-arg GITHUB_TOKEN=$env:GITHUB_TOKEN -t mcp-bsl-server:latest .
```

### Порт уже занят

```
Error: port is already allocated
```

**Решение**: Используйте другой порт:
```powershell
docker run -p 9091:9090 ...
```

или остановите конфликтующий контейнер:
```powershell
docker ps
docker stop <container_id>
```

### Путь не найден

```
Error: path not found: D:\My Projects\Projects 1C
```

**Решение**: Проверьте путь и используйте правильный синтаксис:
```powershell
# Windows PowerShell
-v 'D:\My Projects\Projects 1C:/workspaces:ro'

# Убедитесь что путь существует
Test-Path 'D:\My Projects\Projects 1C'
```

### Контейнер не запускается

```powershell
# Просмотр логов
docker logs <container_name>

# Запуск в интерактивном режиме
docker run --rm -it mcp-bsl-server:latest
```

## Интеграция с Cursor IDE

См. файл `mcp-config.json` для примеров конфигурации.

Базовая конфигурация для stdio:

```json
{
  "mcpServers": {
    "bsl-docker-stdio": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "--network",
        "host",
        "-v",
        "D:\\My Projects\\Projects 1C:/workspaces",
        "mcp-bsl-server:latest"
      ],
      "env": {
        "MCP_TRANSPORT": "stdio"
      }
    }
  }
}
```

## Полезные команды

```powershell
# Список запущенных контейнеров
docker ps

# Список всех контейнеров (включая остановленные)
docker ps -a

# Просмотр логов в реальном времени
docker logs -f <container_name>

# Выполнить команду в контейнере
docker exec -it <container_name> bash

# Просмотр использования ресурсов
docker stats <container_name>

# Очистка неиспользуемых образов
docker image prune

# Очистка всего (осторожно!)
docker system prune -a
```

## Рекомендации

1. **Всегда используйте тег версии** для production:
   ```powershell
   mcp-bsl-server:v0.1.0
   ```

2. **Монтируйте с правами на запись** для поддержки форматирования:
   ```powershell
   -v 'path:/workspaces'
   ```

3. **Используйте Docker Compose** для полного стека с мониторингом

4. **Храните логи** для отладки:
   ```powershell
   docker logs mcp-bsl-server > logs.txt
   ```

5. **Регулярно обновляйте образ** при изменении кода:
   ```powershell
   docker-compose down
   docker-compose build --no-cache
   docker-compose up -d
   ```

## Ссылки

- [Dockerfile](./Dockerfile)
- [docker-compose.yml](./docker-compose.yml)
- [mcp-config.json](./mcp-config.json)
- [README.md](./README.md)

