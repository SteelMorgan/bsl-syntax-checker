# Web UI Documentation

## Overview

MCP BSL Server Web UI предоставляет удобный веб-интерфейс для мониторинга и управления сервером.

**URL**: http://localhost:9090 (по умолчанию)

---

## Главная страница

### Компоненты

```
┌─────────────────────────────────────────────────────────┐
│  🐳 MCP BSL Server                                      │
│  Model Context Protocol Server for BSL Language Server │
│  [Docker Only]                                          │
├─────────────────────────────────────────────────────────┤
│  ✓ Server Running                                       │
│    Web UI always available on port 9090                │
├─────────────────────────────────────────────────────────┤
│  📡 Current Configuration                               │
│    Deployment:    Docker Container (mandatory)          │
│    Web UI Port:   9090 (this page)                     │
│    MCP Transport: http                                  │
│    MCP Port:      8080 (http/sse/ndjson)               │
│    Version:       0.1.0-SNAPSHOT                        │
├─────────────────────────────────────────────────────────┤
│  🚀 Quick Actions                                       │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐      │
│  │ 📚 Swagger  │ │ ❤️  Health  │ │ 📊 Prometheus│      │
│  │     UI      │ │    Check    │ │              │      │
│  └─────────────┘ └─────────────┘ └─────────────┘      │
│  ┌─────────────┐                                        │
│  │ 🔍 Status   │                                        │
│  │   (JSON)    │                                        │
│  └─────────────┘                                        │
└─────────────────────────────────────────────────────────┘
```

---

## Кнопки Quick Actions

### 1. 📚 Swagger UI
**URL**: `/swagger-ui/index.html`

**Назначение**: Интерактивная документация API

**Функции**:
- Просмотр всех доступных endpoints
- Тестирование API непосредственно из браузера
- Просмотр схем запросов/ответов
- Примеры использования

**Цвет**: Зелёный (Swagger brand color)

---

### 2. ❤️ Health Check
**URL**: `/actuator/health`

**Назначение**: Проверка здоровья сервера

**Ответ** (JSON):
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**Цвет**: Зелёный (health/success indicator)

---

### 3. 📊 Prometheus
**URL**: `/actuator/prometheus`

**Назначение**: Метрики для мониторинга

**Формат**: Prometheus text format

**Метрики**:
- JVM memory usage
- CPU usage
- HTTP request counts
- Response times
- Custom BSL session pool metrics

**Цвет**: Красный (Prometheus brand color)

**Использование**:
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'mcp-bsl-server'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['mcp-bsl-server:9090']
```

---

### 4. 🔍 Status (JSON)
**URL**: `/status`

**Назначение**: Детальная информация о сервере

**Ответ** (JSON):
```json
{
  "status": "running",
  "timestamp": "2025-10-23T14:54:37.443563382Z",
  "version": "0.1.0-SNAPSHOT",
  "name": "mcp-bsl-server",
  "deployment": "docker-only",
  "mcp": {
    "transport": "http",
    "port": 8080,
    "requiresHttpServer": true
  },
  "webUI": {
    "swagger": "/swagger-ui/index.html",
    "actuator": "/actuator",
    "prometheus": "/actuator/prometheus"
  }
}
```

**Цвет**: Синий (information/primary)

---

## Динамическая загрузка данных

Web UI автоматически загружает и отображает актуальную информацию при открытии страницы:

```javascript
fetch('/status')
    .then(response => response.json())
    .then(data => {
        // Обновление транспорта
        document.getElementById('transport').textContent = data.mcp?.transport;
        
        // Обновление порта MCP
        document.getElementById('mcp-port').textContent = 
            data.mcp?.port ? `${data.mcp.port} (http/sse/ndjson)` : 'N/A (stdio mode)';
        
        // Обновление версии
        document.getElementById('version').textContent = data.version;
    });
```

---

## Дизайн

### Цветовая схема

| Элемент | Цвет | Назначение |
|---------|------|------------|
| **Фон градиент** | #667eea → #764ba2 | Современный, привлекательный |
| **Карточка статуса** | #4CAF50 (зелёный) | Успешный статус |
| **Swagger кнопка** | #85D300 (зелёный) | Swagger brand |
| **Health кнопка** | #4CAF50 (зелёный) | Здоровье |
| **Prometheus кнопка** | #E6522C (красный) | Prometheus brand |
| **Status кнопка** | #2196F3 (синий) | Информация |

### Анимации

- **Hover эффект**: Кнопка поднимается на 3px с увеличенной тенью
- **Active эффект**: Кнопка опускается на 1px при клике
- **Transition**: Плавное изменение за 0.3s

### Адаптивность

```css
grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
```

Кнопки автоматически адаптируются под размер экрана:
- **Desktop**: 4 кнопки в ряд
- **Tablet**: 2 кнопки в ряд
- **Mobile**: 1 кнопка в ряд

---

## Использование

### Локальный запуск

```powershell
.\gradlew.bat bootRun
```

Откройте: http://localhost:9090

### Docker запуск

```powershell
docker run -d -p 9090:9090 -p 8080:8080 mcp-bsl-server:latest
```

Откройте: http://localhost:9090

### Docker Compose

```yaml
services:
  mcp-bsl-server:
    image: mcp-bsl-server:latest
    ports:
      - "9090:9090"  # Web UI
      - "8080:8080"  # MCP API
```

Откройте: http://localhost:9090

---

## Примеры использования

### 1. Проверка статуса сервера

1. Откройте http://localhost:9090
2. Посмотрите на раздел "📡 Current Configuration"
3. Убедитесь, что:
   - ✅ Статус: "Server Running"
   - ✅ Transport: отображается корректно
   - ✅ Version: соответствует ожидаемой

### 2. Тестирование API

1. Нажмите кнопку **📚 Swagger UI**
2. Выберите endpoint (например, `/api/analyze`)
3. Нажмите "Try it out"
4. Заполните параметры
5. Нажмите "Execute"
6. Просмотрите результат

### 3. Мониторинг здоровья

1. Нажмите кнопку **❤️ Health Check**
2. Проверьте `"status": "UP"`
3. Просмотрите детали по каждому компоненту

### 4. Сбор метрик

1. Нажмите кнопку **📊 Prometheus**
2. Скопируйте метрики
3. Настройте Grafana dashboard
4. Мониторьте производительность

---

## Troubleshooting

### Проблема: Страница не загружается

**Симптомы**:
```
ERR_CONNECTION_REFUSED
```

**Решение**:
```powershell
# Проверьте, что контейнер запущен
docker ps | Select-String "mcp-bsl"

# Проверьте логи
docker logs mcp-bsl-server

# Проверьте порт
netstat -an | Select-String "9090"
```

### Проблема: Кнопки не работают

**Симптомы**: Клик по кнопке не открывает страницу

**Решение**:
1. Откройте консоль браузера (F12)
2. Проверьте ошибки JavaScript
3. Убедитесь, что `/status` отвечает:
   ```powershell
   Invoke-RestMethod http://localhost:9090/status
   ```

### Проблема: Данные не загружаются

**Симптомы**: "Loading..." не меняется на реальные данные

**Решение**:
```powershell
# Проверьте endpoint
Invoke-RestMethod http://localhost:9090/status

# Проверьте формат ответа
curl http://localhost:9090/status | jq .
```

---

## Будущие улучшения

### v0.2.0
- [ ] Real-time метрики на главной странице
- [ ] График загрузки CPU/Memory
- [ ] История последних запросов

### v0.3.0
- [ ] Тёмная тема
- [ ] Интерактивный лог-просмотрщик
- [ ] WebSocket для live updates

### v1.0.0
- [ ] Админ панель для управления
- [ ] User authentication
- [ ] Multi-language support (EN/RU)

---

## Технические детали

### HTML/CSS/JS

- **Чистый HTML5**: Без фреймворков
- **Vanilla JavaScript**: Fetch API для загрузки данных
- **CSS Grid**: Адаптивная сетка для кнопок
- **Градиенты**: Современный visual style
- **Иконки**: Unicode emoji (📚, ❤️, 📊, 🔍)

### Производительность

- **Размер**: ~3 KB (минимальный)
- **Загрузка**: < 100ms (localhost)
- **API вызовы**: 1 (при загрузке страницы)
- **Кэширование**: Static resources

### Безопасность

- **No external dependencies**: Нет CDN, всё локально
- **Read-only**: Только просмотр, нет изменений
- **CORS**: Настроено для localhost
- **No authentication** (по требованиям проекта)

---

## Ссылки

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Prometheus](https://prometheus.io/docs/introduction/overview/)
- [Swagger/OpenAPI](https://swagger.io/specification/)
- [BSL Language Server](https://github.com/1c-syntax/bsl-language-server)

