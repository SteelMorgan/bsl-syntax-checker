# Grafana: Изменения и новые возможности

## ✅ Что было сделано

### 1. Анонимный доступ (без авторизации)

**По умолчанию Grafana работает БЕЗ авторизации!**

- Просто откройте http://localhost:3000 — и вы сразу в интерфейсе
- Никаких логинов/паролей не требуется
- Роль анонимного пользователя: **Editor** (можно создавать и редактировать дашборды)

**Включить авторизацию обратно:** отредактируйте `grafana/grafana.ini`, измените `enabled = true` на `enabled = false` в секции `[auth.anonymous]`, перезапустите Grafana.

### 2. Дефолтный дашборд "MCP BSL Server - Логи"

Автоматически загружается при старте и показывает:

| Панель | Описание |
|--------|----------|
| **Логи по уровням** | Временной график: INFO, WARN, ERROR |
| **Все логи** | Полный список с поиском и фильтрацией |
| **Распределение** | Круговая диаграмма по уровням |
| **Ошибки/предупреждения** | Только ERROR и WARN |

**Прямая ссылка:** http://localhost:3000/d/mcp-bsl-logs

### 3. Обновлённая конфигурация

#### Файлы изменены:

1. **`docker-compose.yml`**
   - Убраны environment переменные (используется `grafana.ini`)
   - Добавлен volume для `grafana.ini`
   - Prometheus порт изменён: `9091:9090`

2. **`grafana/grafana.ini`** (новый файл)
   - Включен анонимный доступ
   - Роль: Editor
   - Организация: Main Org.
   - Дефолтный дашборд: `mcp-bsl-logs.json`

3. **`grafana/provisioning/datasources/datasources.yml`**
   - Добавлены UID для Loki и Prometheus
   - Добавлен `maxLines: 1000` для Loki

4. **`grafana/provisioning/dashboards/mcp-bsl-logs.json`** (новый файл)
   - 4 панели с логами
   - Auto-refresh каждые 5 секунд
   - Временной диапазон: последние 15 минут

#### Документация:

- **`grafana/README.md`** — подробное руководство по настройке
- **`docs/GRAFANA_QUICKSTART.md`** — быстрый старт и примеры запросов
- **`README.md`** — обновлена таблица с ссылками

## 🚀 Как использовать

### Запуск с Grafana:

```powershell
# Запустите полный стек
docker-compose up -d

# Откройте Grafana (без авторизации)
Start-Process "http://localhost:3000"

# Или откройте дашборд напрямую
Start-Process "http://localhost:3000/d/mcp-bsl-logs"
```

### Доступные сервисы:

| Сервис | URL | Описание |
|--------|-----|----------|
| MCP Server | http://localhost:9090 | Web UI + API |
| Grafana | http://localhost:3000 | Логи и метрики (без авторизации) |
| Дашборд логов | http://localhost:3000/d/mcp-bsl-logs | Дефолтный дашборд |
| Prometheus | http://localhost:9091 | Метрики (raw data) |
| Loki | http://localhost:3100 | API для логов |

## 📊 Примеры запросов LogQL

### В Grafana Explore или на дашборде:

```logql
# Все логи MCP сервера
{job="mcp-bsl-server"}

# Только ошибки
{job="mcp-bsl-server"} |= "ERROR"

# Ошибки и предупреждения
{job="mcp-bsl-server"} |~ "ERROR|WARN"

# Поиск по тексту "session"
{job="mcp-bsl-server"} |= "session"

# Логи с timestamp за последние 5 минут
{job="mcp-bsl-server"} [5m]

# Подсчёт ошибок
count_over_time({job="mcp-bsl-server"} |= "ERROR" [$__interval])

# Логи с конкретным уровнем
{job="mcp-bsl-server"} | json | level="ERROR"
```

## 🔧 Управление авторизацией

### Включить авторизацию:

1. Откройте `grafana/grafana.ini`
2. Найдите раздел `[auth.anonymous]`
3. Измените:
   ```ini
   [auth.anonymous]
   enabled = false  # Было: true
   ```
4. Перезапустите:
   ```powershell
   docker-compose restart grafana
   ```
5. Теперь при входе нужен логин: `admin`, пароль: `admin`

### Отключить авторизацию (вернуть как было):

1. В `grafana/grafana.ini` измените обратно:
   ```ini
   [auth.anonymous]
   enabled = true  # Было: false
   ```
2. Перезапустите Grafana

## 📚 Документация

- **`grafana/README.md`** — полное руководство по Grafana
- **`docs/GRAFANA_QUICKSTART.md`** — быстрый старт и примеры
- **`docs/GRAFANA_SETUP.md`** — настройка источников данных и дашбордов

## ✨ Особенности

1. **Анонимный доступ по умолчанию** — открывайте и используйте без логина
2. **Автоматический дашборд** — логи доступны сразу после запуска
3. **Легко включить/отключить авторизацию** — один параметр в конфиге
4. **Роль Editor** — анонимные пользователи могут создавать дашборды
5. **Auto-refresh** — логи обновляются каждые 5 секунд

## 🐛 Troubleshooting

### Нет логов на дашборде?

```powershell
# Проверьте MCP Server
docker ps | Select-String "mcp-bsl-server-checker"

# Проверьте Loki
Invoke-RestMethod "http://localhost:3100/loki/api/v1/label/job/values"
# Должно вернуть: ["mcp-bsl-server"]

# Проверьте подключение в Grafana
# Settings → Data Sources → Loki → Test
```

### Требует авторизацию?

```powershell
# Проверьте конфиг
docker exec mcp-bsl-grafana cat /etc/grafana/grafana.ini | Select-String "auth.anonymous"

# Должно быть: enabled = true
# Если нет, пересоздайте контейнер:
docker-compose stop grafana
docker-compose rm -f grafana
docker-compose up -d grafana
```

## 📝 Итого

✅ Grafana работает БЕЗ авторизации по умолчанию  
✅ Дефолтный дашборд с логами автоматически загружается  
✅ Роль Editor для анонимных пользователей  
✅ Легко включить авторизацию обратно (один параметр)  
✅ Обновлена документация и README  

**Открыть Grafana:** http://localhost:3000  
**Открыть дашборд логов:** http://localhost:3000/d/mcp-bsl-logs

