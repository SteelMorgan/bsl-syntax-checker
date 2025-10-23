# MCP Configuration Examples

Примеры конфигурации MCP BSL Server для различных сценариев использования.

## Базовое расположение конфигурации

**Windows (Cursor IDE)**:
```
%APPDATA%\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json
```

**Полный путь (обычно)**:
```
C:\Users\<USERNAME>\AppData\Roaming\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json
```

## 1. STDIO режим (рекомендуется для Cursor)

### Базовая конфигурация

```json
{
  "mcpServers": {
    "bsl-language-server": {
      "command": "java",
      "args": [
        "-jar",
        "D:\\Projects\\mcp-bsl-server\\build\\libs\\mcp-bsl-server-0.1.0-SNAPSHOT.jar"
      ],
      "env": {
        "TRANSPORT_MODE": "stdio",
        "LOGGING_ENABLED": "false",
        "MOUNT_HOST_ROOT": "D:\\Projects\\1C",
        "BSL_JAR_PATH": "D:\\Tools\\bsl-language-server.jar",
        "BSL_MAX_HEAP": "4g"
      },
      "disabled": false
    }
  }
}
```

**Когда использовать**:
- Основной режим для работы в Cursor IDE
- Минимальная латентность
- Прямое взаимодействие процессов

### С увеличенной памятью для больших проектов

```json
{
  "mcpServers": {
    "bsl-language-server": {
      "command": "java",
      "args": [
        "-Xmx2g",
        "-jar",
        "D:\\Projects\\mcp-bsl-server\\build\\libs\\mcp-bsl-server-0.1.0-SNAPSHOT.jar"
      ],
      "env": {
        "TRANSPORT_MODE": "stdio",
        "LOGGING_ENABLED": "false",
        "MOUNT_HOST_ROOT": "D:\\Projects\\1C",
        "BSL_JAR_PATH": "D:\\Tools\\bsl-language-server.jar",
        "BSL_MAX_HEAP": "8g",
        "BSL_POOL_MAX_SIZE": "10",
        "BSL_POOL_TTL": "120"
      },
      "disabled": false
    }
  }
}
```

**Параметры**:
- `-Xmx2g` - память для MCP сервера (2 GB)
- `BSL_MAX_HEAP=8g` - память для BSL LS процессов (8 GB)
- `BSL_POOL_MAX_SIZE=10` - до 10 одновременных сессий
- `BSL_POOL_TTL=120` - TTL сессий 120 минут

## 2. HTTP режим (для тестирования)

### С логированием в консоль

```json
{
  "mcpServers": {
    "bsl-language-server-http": {
      "command": "java",
      "args": [
        "-jar",
        "D:\\Projects\\mcp-bsl-server\\build\\libs\\mcp-bsl-server-0.1.0-SNAPSHOT.jar"
      ],
      "env": {
        "TRANSPORT_MODE": "http",
        "SERVER_PORT": "8080",
        "LOGGING_ENABLED": "false",
        "MOUNT_HOST_ROOT": "D:\\Projects\\1C",
        "BSL_JAR_PATH": "D:\\Tools\\bsl-language-server.jar"
      },
      "disabled": false
    }
  }
}
```

**Доступ к API**:
- REST API: `http://localhost:8080/api/*`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Status: `http://localhost:8080/status`
- Health: `http://localhost:8080/actuator/health`

### С Loki логированием

```json
{
  "mcpServers": {
    "bsl-language-server-http": {
      "command": "java",
      "args": [
        "-jar",
        "D:\\Projects\\mcp-bsl-server\\build\\libs\\mcp-bsl-server-0.1.0-SNAPSHOT.jar"
      ],
      "env": {
        "TRANSPORT_MODE": "http",
        "SERVER_PORT": "8080",
        "LOGGING_ENABLED": "true",
        "logging.loki.url": "http://localhost:3100/loki/api/v1/push",
        "MOUNT_HOST_ROOT": "D:\\Projects\\1C",
        "BSL_JAR_PATH": "D:\\Tools\\bsl-language-server.jar"
      },
      "disabled": false
    }
  }
}
```

**Требует**:
- Loki запущен на `localhost:3100`
- Можно запустить через: `docker-compose up loki -d`

## 3. Docker режим

### Базовая конфигурация

```json
{
  "mcpServers": {
    "bsl-language-server-docker": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "--network",
        "host",
        "-v",
        "D:\\Projects\\1C:/workspaces:ro",
        "mcp-bsl-server:latest"
      ],
      "env": {
        "TRANSPORT_MODE": "stdio"
      },
      "disabled": false
    }
  }
}
```

**Преимущества**:
- Полная изоляция
- Не требует локального Java
- BSL LS уже включён в образ

### С кастомным портом (HTTP режим в контейнере)

```json
{
  "mcpServers": {
    "bsl-language-server-docker-http": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-d",
        "-p",
        "18080:8080",
        "-v",
        "D:\\Projects\\1C:/workspaces:ro",
        "--name",
        "mcp-bsl-http",
        "mcp-bsl-server:latest"
      ],
      "env": {
        "TRANSPORT_MODE": "http",
        "SERVER_PORT": "8080"
      },
      "disabled": false
    }
  }
}
```

**Доступ**: `http://localhost:18080/swagger-ui/index.html`

### С volume для логов

```json
{
  "mcpServers": {
    "bsl-language-server-docker-logs": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "--network",
        "host",
        "-v",
        "D:\\Projects\\1C:/workspaces:ro",
        "-v",
        "D:\\Logs\\mcp-bsl:/logs",
        "mcp-bsl-server:latest"
      ],
      "env": {
        "TRANSPORT_MODE": "stdio",
        "LOGGING_ENABLED": "true",
        "logging.file.path": "/logs"
      },
      "disabled": false
    }
  }
}
```

## 4. Минимальная конфигурация (без BSL LS)

```json
{
  "mcpServers": {
    "bsl-language-server-minimal": {
      "command": "java",
      "args": [
        "-jar",
        "D:\\Projects\\mcp-bsl-server\\build\\libs\\mcp-bsl-server-0.1.0-SNAPSHOT.jar"
      ],
      "env": {
        "TRANSPORT_MODE": "stdio",
        "LOGGING_ENABLED": "false"
      },
      "disabled": false
    }
  }
}
```

**Когда использовать**:
- Быстрое тестирование API
- Проверка интеграции с Cursor
- BSL LS не установлен (возвращает mock-данные)

## 5. Несколько конфигураций одновременно

```json
{
  "mcpServers": {
    "bsl-main-project": {
      "command": "java",
      "args": ["-jar", "D:\\...\\mcp-bsl-server-0.1.0-SNAPSHOT.jar"],
      "env": {
        "TRANSPORT_MODE": "stdio",
        "MOUNT_HOST_ROOT": "D:\\Projects\\MainERP",
        "BSL_JAR_PATH": "D:\\Tools\\bsl-language-server.jar"
      },
      "disabled": false
    },
    "bsl-test-project": {
      "command": "java",
      "args": ["-jar", "D:\\...\\mcp-bsl-server-0.1.0-SNAPSHOT.jar"],
      "env": {
        "TRANSPORT_MODE": "stdio",
        "SERVER_PORT": "8081",
        "MOUNT_HOST_ROOT": "D:\\Projects\\TestProject",
        "BSL_JAR_PATH": "D:\\Tools\\bsl-language-server.jar"
      },
      "disabled": false
    }
  }
}
```

**Использование**:
- Разные MCP серверы для разных проектов
- Изолированные session pools
- Независимые конфигурации

## 6. Production-ready конфигурация

```json
{
  "mcpServers": {
    "bsl-language-server-production": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "--network",
        "host",
        "-v",
        "D:\\Projects\\1C:/workspaces:ro",
        "--memory",
        "4g",
        "--cpus",
        "2",
        "--restart",
        "unless-stopped",
        "mcp-bsl-server:latest"
      ],
      "env": {
        "TRANSPORT_MODE": "stdio",
        "LOGGING_ENABLED": "true",
        "logging.loki.url": "http://loki.internal:3100/loki/api/v1/push",
        "BSL_MAX_HEAP": "6g",
        "BSL_POOL_MAX_SIZE": "20",
        "BSL_POOL_TTL": "180"
      },
      "disabled": false
    }
  }
}
```

**Особенности**:
- Ограничение ресурсов (4 GB RAM, 2 CPU)
- Автоматический перезапуск
- Централизованное логирование
- Увеличенный session pool

## Переменные окружения

### Обязательные
Нет обязательных переменных - все имеют значения по умолчанию.

### Рекомендуемые

| Переменная | Значение по умолчанию | Описание |
|-----------|----------------------|----------|
| `TRANSPORT_MODE` | `stdio` | Режим транспорта: `stdio`, `http` |
| `MOUNT_HOST_ROOT` | - | Корневая директория для path mapping |
| `BSL_JAR_PATH` | - | Путь к BSL Language Server JAR |

### Опциональные

| Переменная | Значение | Описание |
|-----------|----------|----------|
| `SERVER_PORT` | `8080` | Порт HTTP сервера |
| `LOGGING_ENABLED` | `true` | Включить Loki logging |
| `logging.loki.url` | `http://localhost:3100/...` | URL Loki |
| `BSL_MAX_HEAP` | `4g` | Heap для BSL LS процессов |
| `BSL_POOL_MAX_SIZE` | `5` | Макс. размер session pool |
| `BSL_POOL_TTL` | `60` | TTL сессий (минуты) |

## Troubleshooting

### MCP сервер не запускается

**Проверка**:
```powershell
# Проверить Java
java -version

# Проверить JAR существует
Test-Path "D:\Path\To\mcp-bsl-server-0.1.0-SNAPSHOT.jar"

# Запустить вручную для диагностики
java -jar "D:\Path\To\mcp-bsl-server-0.1.0-SNAPSHOT.jar"
```

### Ошибки path mapping

**Решение**: Убедитесь, что:
1. `MOUNT_HOST_ROOT` задан корректно
2. Пути в запросах находятся внутри `MOUNT_HOST_ROOT`
3. Используются абсолютные пути с правильными разделителями

### Docker контейнер не видит файлы

**Решение**:
```json
"args": [
  "run",
  "--rm",
  "-i",
  "-v",
  "D:\\Projects\\1C:/workspaces:ro",  // :ro = read-only
  "mcp-bsl-server:latest"
]
```

Проверить mount:
```powershell
docker run --rm -v "D:\Projects\1C:/workspaces:ro" alpine ls -la /workspaces
```

## Дополнительные ресурсы

- [CURSOR_INTEGRATION.md](docs/CURSOR_INTEGRATION.md) - Подробное руководство
- [TRANSPORTS.md](docs/TRANSPORTS.md) - Описание транспортов
- [README.md](README.md) - Основная документация
- [QUICKSTART.md](QUICKSTART.md) - Быстрый старт

## Шаблоны для копирования

### Быстрый старт (копировать в Cursor config)

```json
{
  "mcpServers": {
    "bsl-language-server": {
      "command": "java",
      "args": ["-jar", "ПУТЬ_К_JAR"],
      "env": {
        "TRANSPORT_MODE": "stdio",
        "MOUNT_HOST_ROOT": "ПУТЬ_К_ПРОЕКТАМ_1C",
        "BSL_JAR_PATH": "ПУТЬ_К_BSL_LS"
      },
      "disabled": false
    }
  }
}
```

**Замените**:
- `ПУТЬ_К_JAR` → `D:\...\mcp-bsl-server-0.1.0-SNAPSHOT.jar`
- `ПУТЬ_К_ПРОЕКТАМ_1C` → `D:\Projects\1C`
- `ПУТЬ_К_BSL_LS` → `D:\Tools\bsl-language-server.jar`

