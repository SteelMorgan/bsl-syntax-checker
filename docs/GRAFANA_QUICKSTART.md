# Grafana Quick Start

## Доступ без авторизации ✅

По умолчанию Grafana настроена на **анонимный доступ**:

1. Откройте http://localhost:3000
2. Вы сразу попадаете в интерфейс (без логина/пароля)
3. Роль по умолчанию: **Editor** — можно создавать и редактировать дашборды

## Дефолтный дашборд "MCP BSL Server - Логи"

Автоматически загружается при старте и содержит 4 панели:

### 1. Временной график логов по уровням
- График с разбивкой по уровням: INFO, WARN, ERROR
- Интервал обновления: 5 секунд
- Цветовая схема: ERROR (красный), WARN (оранжевый), INFO (синий)

### 2. Все логи MCP BSL Server
- Полный список всех логов
- Поиск по тексту
- Фильтрация по времени
- Просмотр деталей (раскрытие JSON)

### 3. Распределение логов по уровням
- Круговая диаграмма (donut chart)
- Показывает процентное соотношение типов логов
- Легенда с абсолютными значениями

### 4. Ошибки и предупреждения
- Только логи уровня ERROR и WARN
- Автообновление каждые 5 секунд
- Сортировка от новых к старым

## Быстрый доступ

- **Главная страница**: http://localhost:3000
- **Дашборд логов**: http://localhost:3000/d/mcp-bsl-logs
- **Список всех дашбордов**: http://localhost:3000/dashboards

## Поиск и фильтрация логов

### Основные запросы LogQL:

```logql
# Все логи MCP сервера
{job="mcp-bsl-server"}

# Только ошибки
{job="mcp-bsl-server"} |= "ERROR"

# Ошибки и предупреждения
{job="mcp-bsl-server"} |~ "ERROR|WARN"

# Поиск по тексту "session"
{job="mcp-bsl-server"} |= "session"

# Подсчёт логов за интервал
count_over_time({job="mcp-bsl-server"} [$__interval])
```

### Фильтры времени:
- Last 5 minutes
- Last 15 minutes (по умолчанию)
- Last 1 hour
- Или выберите custom range

## Включение авторизации (опционально)

Если вам нужна авторизация:

1. Откройте `grafana/grafana.ini`
2. Найдите раздел `[auth.anonymous]`
3. Измените `enabled = true` на `enabled = false`
4. Сохраните файл
5. Перезапустите Grafana:
   ```powershell
   docker-compose restart grafana
   ```

После этого при входе потребуется:
- **Логин**: `admin`
- **Пароль**: `admin`

Grafana предложит сменить пароль при первом входе (можно пропустить).

## Создание своих дашбордов

С правами Editor вы можете:

1. Создавать новые дашборды:
   - Click "+" → "Dashboard" → "Add visualization"
   - Выберите источник данных: **Loki** (для логов) или **Prometheus** (для метрик)
   - Настройте запрос и визуализацию
   - Сохраните дашборд

2. Редактировать существующие:
   - Откройте дашборд → "Settings" (шестерёнка)
   - Добавьте/удалите панели
   - Измените запросы
   - Сохраните изменения

3. Экспортировать дашборды:
   - Settings → JSON Model → Copy to Clipboard
   - Сохраните в файл `grafana/provisioning/dashboards/my-dashboard.json`
   - Перезапустите Grafana для auto-load

## Troubleshooting

### Нет логов на дашборде

1. Проверьте, что MCP Server запущен:
   ```powershell
   docker ps | Select-String "mcp-bsl-server-checker"
   ```

2. Проверьте, что Loki получает логи:
   ```powershell
   Invoke-RestMethod "http://localhost:3100/loki/api/v1/label/job/values"
   ```
   Должно вернуть `["mcp-bsl-server"]`

3. Проверьте подключение Loki в Grafana:
   - Settings → Data Sources → Loki → "Test"
   - Должно быть "Data source is working"

### Дашборд не загружается автоматически

1. Проверьте файл дашборда:
   ```powershell
   Test-Path grafana/provisioning/dashboards/mcp-bsl-logs.json
   ```

2. Проверьте логи Grafana:
   ```powershell
   docker logs mcp-bsl-grafana | Select-String "dashboard"
   ```

3. Перезапустите Grafana:
   ```powershell
   docker-compose restart grafana
   ```

### Ошибка "Anonymous access denied"

Проверьте, что `grafana.ini` смонтирован правильно:

```powershell
docker exec mcp-bsl-grafana cat /etc/grafana/grafana.ini | Select-String "auth.anonymous"
```

Должно показать:
```ini
[auth.anonymous]
enabled = true
```

Если показывает `false` или секция отсутствует:
1. Проверьте файл `grafana/grafana.ini`
2. Пересоздайте контейнер:
   ```powershell
   docker-compose stop grafana
   docker-compose rm -f grafana
   docker-compose up -d grafana
   ```

## Полезные ссылки

- [Grafana Documentation](https://grafana.com/docs/grafana/latest/)
- [Loki Query Language (LogQL)](https://grafana.com/docs/loki/latest/logql/)
- [Prometheus Query Language (PromQL)](https://prometheus.io/docs/prometheus/latest/querying/basics/)

