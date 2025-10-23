# Настройка Grafana для просмотра логов и метрик

## 🎯 Зачем нужна Grafana?

**Prometheus** — это текстовый источник метрик (формат для машинного чтения).  
**Grafana** — это графический интерфейс для визуализации метрик и логов.

---

## 🚀 Быстрый старт

### 1. Запустите полный стек

```powershell
# Установите GitHub токен (если нужен)
$env:GITHUB_TOKEN = (Get-Content '.secrets\github_token.txt' -Raw).Trim()

# Запустите все сервисы
docker-compose up -d
```

### 2. Откройте Grafana

Откройте в браузере: **http://localhost:3000**

**Логин:** `admin`  
**Пароль:** `admin` (при первом входе предложит сменить)

---

## 📊 Доступные источники данных

После запуска `docker-compose` автоматически настроены:

### 1. **Loki** (для логов)
- **URL:** http://loki:3100
- **Назначение:** Просмотр логов приложения

### 2. **Prometheus** (для метрик)
- **URL:** http://prometheus:9090
- **Назначение:** Просмотр метрик (запросы, время ответа, использование памяти)

---

## 🔍 Примеры запросов

### Просмотр логов в Loki

1. Откройте **Explore** (иконка компаса слева)
2. Выберите источник **Loki**
3. Введите запрос:

```logql
{app="mcp-bsl-server"}
```

**Фильтры:**
- Только ошибки: `{app="mcp-bsl-server"} |= "ERROR"`
- Логи за последний час: установите `Last 1 hour` вверху
- Логи конкретного класса: `{app="mcp-bsl-server"} |= "BslCliService"`

### Просмотр метрик в Prometheus

1. Откройте **Explore**
2. Выберите источник **Prometheus**
3. Введите запрос:

```promql
# Количество HTTP запросов
http_server_requests_seconds_count

# Использование памяти JVM
jvm_memory_used_bytes

# Время ответа API
rate(http_server_requests_seconds_sum[5m])
```

---

## 📈 Создание дашбордов

### Импорт готового дашборда для Spring Boot

1. Нажмите **+** → **Import Dashboard**
2. Введите ID: `11378` (Spring Boot 2.1 System Monitor)
3. Нажмите **Load**
4. Выберите источник данных **Prometheus**
5. Нажмите **Import**

### Создание собственного дашборда

1. Нажмите **+** → **Dashboard**
2. **Add visualization**
3. Выберите источник данных (Loki или Prometheus)
4. Настройте запрос
5. Выберите тип визуализации (график, таблица, калибр)
6. **Save**

---

## 🔧 Полезные дашборды

### Для Spring Boot приложений:

| ID | Название | Описание |
|----|----------|----------|
| 11378 | Spring Boot 2.1 System Monitor | JVM, HTTP, DB метрики |
| 4701 | JVM (Micrometer) | Детальная информация о JVM |
| 13639 | Spring Boot Logback Logs | Визуализация логов |

---

## 🎨 Панели для мониторинга MCP BSL Server

### 1. HTTP Requests Rate
```promql
rate(http_server_requests_seconds_count{uri="/api/analyze"}[5m])
```

### 2. Average Response Time
```promql
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])
```

### 3. Error Rate
```promql
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

### 4. Active Sessions
```promql
bsl_sessions_active
```

### 5. Memory Usage
```promql
jvm_memory_used_bytes{area="heap"}
```

---

## 🐛 Поиск проблем через логи

### Найти ошибки за последний час:
```logql
{app="mcp-bsl-server"} |= "ERROR" | json
```

### Найти медленные запросы:
```logql
{app="mcp-bsl-server"} |~ "took [0-9]{4,}ms"
```

### Логи конкретного endpoint:
```logql
{app="mcp-bsl-server"} |= "/api/analyze"
```

---

## ⚙️ Настройка алертов

### 1. Создайте Alert Rule

1. Откройте **Alerting** → **Alert rules**
2. **New alert rule**
3. Настройте условие:
   ```promql
   rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
   ```
4. Установите порог и действия

### 2. Настройте канал уведомлений

1. **Alerting** → **Contact points**
2. **New contact point**
3. Выберите тип (Email, Slack, Telegram)
4. Настройте параметры

---

## 📍 Полезные ссылки

- [Grafana Documentation](https://grafana.com/docs/)
- [Loki Query Language](https://grafana.com/docs/loki/latest/logql/)
- [Prometheus Query Language](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)

---

## 🔄 Обновление конфигурации

Конфигурация Grafana хранится в:
- `grafana/provisioning/datasources/` — источники данных
- `grafana/provisioning/dashboards/` — дашборды

После изменений:
```powershell
docker-compose restart grafana
```

---

**Обновлено:** Октябрь 2024

