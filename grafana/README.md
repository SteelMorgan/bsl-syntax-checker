# Grafana Configuration

## Анонимный доступ (по умолчанию)

По умолчанию Grafana настроена на **анонимный доступ** без авторизации:
- Открывайте http://localhost:3000 и сразу попадаете в интерфейс
- Роль по умолчанию: **Editor** (можно создавать и редактировать дашборды)
- Дефолтный дашборд: **MCP BSL Server - Логи**

## Включение авторизации

Если вам нужна авторизация, отредактируйте `grafana/grafana.ini`:

```ini
[auth.anonymous]
enabled = false  # Было: true
```

После этого перезапустите Grafana:
```powershell
docker-compose restart grafana
```

Теперь для входа потребуется:
- **Логин**: `admin`
- **Пароль**: `admin`

## Дашборды

### MCP BSL Server - Логи (по умолчанию)

Автоматически загружается при старте и содержит:

1. **Логи по уровням** - временной график с разбивкой по уровням (INFO, WARN, ERROR)
2. **Все логи MCP BSL Server** - полный список логов с возможностью поиска и фильтрации
3. **Распределение логов по уровням** - круговая диаграмма
4. **Ошибки и предупреждения** - только ERROR и WARN логи

### Создание своих дашбордов

Вы можете создавать свои дашборды через UI Grafana. Они сохранятся в volume `grafana-data`.

Чтобы добавить дашборд в provisioning (автоматическая загрузка):
1. Создайте JSON файл в `grafana/provisioning/dashboards/`
2. Перезапустите Grafana: `docker-compose restart grafana`

## Источники данных

### Loki
- URL: http://loki:3100
- По умолчанию: Да
- Назначение: Логи приложений

### Prometheus
- URL: http://prometheus:9090
- По умолчанию: Нет
- Назначение: Метрики (CPU, память, запросы и т.д.)

## Запросы в Loki

Примеры LogQL запросов для поиска логов:

```logql
# Все логи MCP сервера
{job="mcp-bsl-server"}

# Только ошибки
{job="mcp-bsl-server"} |= "ERROR"

# Ошибки и предупреждения
{job="mcp-bsl-server"} |~ "ERROR|WARN"

# Логи за последние 5 минут с фильтром
{job="mcp-bsl-server"} |= "session" [5m]

# Подсчёт ошибок за интервал
count_over_time({job="mcp-bsl-server"} |= "ERROR" [$__interval])
```

## Troubleshooting

### Grafana не показывает логи

1. Проверьте, что Loki запущен:
   ```powershell
   docker ps | Select-String "loki"
   ```

2. Проверьте соединение с Loki в Grafana:
   - Settings → Data Sources → Loki → Test

3. Проверьте логи Grafana:
   ```powershell
   docker logs mcp-bsl-grafana
   ```

### Дашборд не загружается автоматически

1. Проверьте наличие файла:
   ```powershell
   Test-Path grafana/provisioning/dashboards/mcp-bsl-logs.json
   ```

2. Перезапустите Grafana:
   ```powershell
   docker-compose restart grafana
   ```

3. Проверьте логи:
   ```powershell
   docker logs mcp-bsl-grafana | Select-String "dashboard"
   ```

