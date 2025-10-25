# 🚀 Быстрый старт MCP BSL Server в Cursor IDE

## ⚡ Автоматическая настройка (рекомендуется)

```powershell
# Запустите скрипт автоматической настройки
.\setup-cursor-mcp.ps1
```

## 🔧 Ручная настройка

### 1. Сборка и запуск

```bash
# Соберите Docker образ
docker build -t mcp-bsl-server:latest .

# Запустите контейнер
docker run --rm -d \
  --name mcp-bsl-server-checker \
  -e MCP_TRANSPORT=http \
  -p 9090:9090 \
  -p 8080:8080 \
  -v "D:\My Projects\Projects 1C:/workspaces:ro" \
  mcp-bsl-server:latest
```

### 2. Настройка Cursor IDE

Скопируйте содержимое файла `cursor-mcp-config.json` в:
```
%APPDATA%\Cursor\User\globalStorage\saoudrizwan.claude-dev\settings\cline_mcp_settings.json
```

### 3. Перезапустите Cursor IDE

## ✅ Проверка работы

1. **Web UI**: http://localhost:9090
2. **Swagger**: http://localhost:9090/swagger-ui
3. **В Cursor IDE**: Должны появиться 5 инструментов BSL

## 🎯 Доступные инструменты

- `bslcheck_analyze` - анализ кода 1C
- `bslcheck_format` - форматирование кода
- `bslcheck_session_start` - запуск сессии
- `bslcheck_session_status` - статус сессии
- `bslcheck_session_stop` - остановка сессии

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

**Статус**: ✅ Все исправления протестированы и работают  
**Версия**: v0.1.0-fixed
