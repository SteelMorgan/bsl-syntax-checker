# Руководство по созданию MCP-серверов на основе шаблонной архитектуры

## Обзор

Данный документ описывает, как использовать архитектуру MCP BSL Server как шаблон для создания других MCP-серверов, сохраняя всю инфраструктуру (логирование, мониторинг, Swagger, поддержка протоколов) и заменяя только функциональность Tools.

## Часть 1: Шаблонная архитектура (неизменяемая)

### 1.1 Основные компоненты архитектуры

#### Spring Boot Application
```kotlin
@SpringBootApplication
@ConfigurationPropertiesScan
class McpBslServerApplication
```

**Что сохраняется:**
- Spring Boot 3.2.5 с Kotlin
- Автоконфигурация через `@ConfigurationPropertiesScan`
- Graceful shutdown
- CORS настройки

#### Dual-Port Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                     MCP Server Container                    │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │          Spring Boot Application                     │   │
│  │                                                      │   │
│  │  ┌────────────────────┐    ┌─────────────────────┐  │   │
│  │  │   Web UI Server    │    │   MCP Transport     │  │   │
│  │  │   (Port 9090)      │    │   (Port 8080)       │  │   │
│  │  │   ALWAYS ACTIVE    │    │   CONDITIONAL       │  │   │
│  │  │                    │    │                     │  │   │
│  │  │ • Swagger UI       │    │ • stdio (stdin/out) │  │   │
│  │  │ • Actuator         │    │ • HTTP REST API     │  │   │
│  │  │ • Prometheus       │    │ • SSE streaming     │  │   │
│  │  │ • Status Page      │    │ • NDJSON chunking   │  │   │
│  │  └────────────────────┘    └─────────────────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

**Что сохраняется:**
- Web UI всегда активен на порту 9090
- MCP Transport условно активен на порту 8080
- Поддержка stdio, HTTP, SSE, NDJSON транспортов

#### Конфигурация (application.yml)
```yaml
# Web UI Server (ALWAYS active)
server:
  port: ${WEB_UI_PORT:9090}

# MCP Transport configuration
mcp:
  transport: ${MCP_TRANSPORT:http}
  port: ${MCP_PORT:8080}

# Logging
logging:
  loki:
    enabled: ${LOGGING_ENABLED:true}
    url: ${LOKI_URL:http://loki:3100/loki/api/v1/push}

# Management endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

# Swagger/OpenAPI
springdoc:
  swagger-ui:
    path: /swagger-ui
    enabled: true
```

**Что сохраняется:**
- Все настройки логирования и мониторинга
- Swagger UI конфигурация
- Actuator endpoints
- CORS настройки

#### Логирование и мониторинг
```xml
<!-- logback-spring.xml -->
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <http>
        <url>${lokiUrl}</url>
    </http>
    <format>
        <label>
            <pattern>app=${appName},host=${HOSTNAME:-localhost},level=%level,job=mcp-bsl-server</pattern>
        </label>
    </format>
</appender>
```

**Что сохраняется:**
- Loki appender для централизованного логирования
- Prometheus metrics
- Grafana интеграция
- Structured logging

#### Docker инфраструктура
```dockerfile
FROM eclipse-temurin:17-jre-alpine AS base

# Install required tools
RUN apk add --no-cache curl wget ca-certificates bash

# Copy application JAR
COPY build/libs/*.jar app.jar

# Environment variables
ENV WEB_UI_PORT=9090
ENV MCP_TRANSPORT=http
ENV LOGGING_ENABLED=true

# Expose ports
EXPOSE 9090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Что сохраняется:**
- Alpine Linux base image
- Java 17 runtime
- Port configuration
- Environment variables

#### Docker Compose стек
```yaml
services:
  mcp-server:
    build: .
    ports:
      - "9090:9090"  # Web UI
      - "8080:8080"  # MCP API
    environment:
      - MCP_TRANSPORT=http
      - LOGGING_ENABLED=true
    depends_on:
      - loki

  loki:
    image: grafana/loki:2.9.3
    ports:
      - "3100:3100"

  grafana:
    image: grafana/grafana:10.2.3
    ports:
      - "3000:3000"
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning

  prometheus:
    image: prom/prometheus:v2.48.1
    ports:
      - "9091:9090"
```

**Что сохраняется:**
- Полный стек мониторинга (Loki + Grafana + Prometheus)
- Network configuration
- Volume mounts
- Service dependencies

### 1.2 Gradle конфигурация
```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic")
    implementation("com.github.loki4j:loki-logback-appender:1.5.1")
    
    // Swagger/OpenAPI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    // Micrometer (metrics)
    implementation("io.micrometer:micrometer-core")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}
```

**Что сохраняется:**
- Все зависимости для логирования и мониторинга
- Spring Boot starter'ы
- Kotlin поддержка
- Swagger/OpenAPI
- Micrometer для метрик

## Часть 2: Функциональность конкретных Tools (изменяемая)

### 2.1 Структура для замены

#### Основные компоненты для замены:

1. **Domain Service** - основная бизнес-логика
2. **DTO Models** - модели данных для API
3. **Controller** - REST API endpoints
4. **Configuration Properties** - настройки домена
5. **External Tool Integration** - интеграция с внешними инструментами

### 2.2 Шаблон замены компонентов

#### Шаг 1: Создание Domain Service

Замените `BslCliService` на ваш доменный сервис:

```kotlin
// Было: BslCliService
@Service
class BslCliService(
    private val properties: BslServerProperties,
    private val outputParser: BslOutputParser
) {
    fun analyze(srcPath: Path, reporters: List<String>, language: String): AnalyzeResult
    fun format(src: Path, inPlace: Boolean): FormatResult
}

// Станет: YourDomainService
@Service
class YourDomainService(
    private val properties: YourDomainProperties,
    private val outputParser: YourOutputParser
) {
    fun processData(input: String, options: Map<String, Any>): ProcessResult
    fun validateData(data: String): ValidationResult
}
```

#### Шаг 2: Создание DTO Models

Замените `ApiModels.kt`:

```kotlin
// Было: BSL-specific DTOs
data class AnalyzeRequest(
    val srcDir: String,
    val reporters: List<String> = listOf("json"),
    val language: String = "ru"
)

data class AnalyzeResponse(
    val summary: AnalyzeSummary,
    val diagnostics: List<Diagnostic>
)

// Станет: Your domain DTOs
data class ProcessRequest(
    val input: String,
    val options: Map<String, Any> = emptyMap(),
    val format: String = "json"
)

data class ProcessResponse(
    val result: String,
    val metadata: ProcessMetadata
)
```

#### Шаг 3: Создание Controller

Замените `BslApiController`:

```kotlin
// Было: BSL API Controller
@RestController
@RequestMapping("/api")
class BslApiController(
    private val bslCliService: BslCliService,
    private val pathMappingService: PathMappingService
) {
    @PostMapping("/analyze")
    fun analyze(@RequestBody request: AnalyzeRequest): ResponseEntity<Any>
    
    @PostMapping("/format")
    fun format(@RequestBody request: FormatRequest): ResponseEntity<Any>
}

// Станет: Your domain API Controller
@RestController
@RequestMapping("/api")
class YourDomainController(
    private val yourDomainService: YourDomainService,
    private val pathMappingService: PathMappingService  // Сохраняется!
) {
    @PostMapping("/process")
    fun process(@RequestBody request: ProcessRequest): ResponseEntity<Any>
    
    @PostMapping("/validate")
    fun validate(@RequestBody request: ValidateRequest): ResponseEntity<Any>
}
```

#### Шаг 4: Создание Configuration Properties

Замените `BslServerProperties`:

```kotlin
// Было: BSL Properties
@ConfigurationProperties(prefix = "bsl")
data class BslServerProperties(
    val jarPath: String = "/opt/bsl/bsl-language-server.jar",
    val jvm: JvmProperties = JvmProperties(),
    val pool: PoolProperties = PoolProperties()
)

// Станет: Your domain Properties
@ConfigurationProperties(prefix = "yourdomain")
data class YourDomainProperties(
    val toolPath: String = "/opt/your-tool/executable",
    val timeout: Duration = Duration.ofMinutes(5),
    val maxConcurrency: Int = 10
)
```

#### Шаг 5: Создание External Tool Integration

Замените `BslProcess` и связанные компоненты:

```kotlin
// Было: BSL Process wrapper
class BslProcess(
    jarPath: Path,
    maxHeap: String,
    workspaceDir: Path,
    mode: BslMode
) {
    fun executeCliCommand(vararg args: String): ProcessResult
    fun start()
    fun sendRequest(request: String): String?
}

// Станет: Your tool Process wrapper
class YourToolProcess(
    toolPath: Path,
    timeout: Duration,
    workspaceDir: Path,
    mode: YourToolMode
) {
    fun executeCommand(vararg args: String): ProcessResult
    fun start()
    fun sendData(data: String): String?
}
```

### 2.3 Примеры замены для разных доменов

#### Пример 1: MCP Server для Python Code Analysis

```kotlin
// Domain Service
@Service
class PythonAnalysisService(
    private val properties: PythonAnalysisProperties,
    private val outputParser: PythonOutputParser
) {
    fun analyzePythonCode(srcPath: Path, linters: List<String>): AnalysisResult {
        // Интеграция с pylint, flake8, mypy, black
    }
    
    fun formatPythonCode(src: Path, formatter: String): FormatResult {
        // Интеграция с black, autopep8
    }
}

// DTOs
data class PythonAnalysisRequest(
    val srcDir: String,
    val linters: List<String> = listOf("pylint", "flake8"),
    val pythonVersion: String = "3.9"
)

data class PythonAnalysisResponse(
    val summary: AnalysisSummary,
    val issues: List<PythonIssue>
)

// Controller
@RestController
@RequestMapping("/api")
class PythonAnalysisController(
    private val pythonAnalysisService: PythonAnalysisService,
    private val pathMappingService: PathMappingService
) {
    @PostMapping("/analyze")
    fun analyze(@RequestBody request: PythonAnalysisRequest): ResponseEntity<Any>
    
    @PostMapping("/format")
    fun format(@RequestBody request: PythonFormatRequest): ResponseEntity<Any>
}
```

#### Пример 2: MCP Server для Database Schema Analysis

```kotlin
// Domain Service
@Service
class DatabaseAnalysisService(
    private val properties: DatabaseProperties,
    private val outputParser: DatabaseOutputParser
) {
    fun analyzeSchema(connectionString: String, schema: String): SchemaAnalysisResult {
        // Анализ структуры БД, индексов, связей
    }
    
    fun validateQueries(queries: List<String>, schema: String): QueryValidationResult {
        // Валидация SQL запросов
    }
}

// DTOs
data class SchemaAnalysisRequest(
    val connectionString: String,
    val schema: String,
    val includeIndexes: Boolean = true,
    val includeConstraints: Boolean = true
)

data class SchemaAnalysisResponse(
    val tables: List<TableInfo>,
    val relationships: List<Relationship>,
    val indexes: List<IndexInfo>
)

// Controller
@RestController
@RequestMapping("/api")
class DatabaseAnalysisController(
    private val databaseAnalysisService: DatabaseAnalysisService
) {
    @PostMapping("/analyze-schema")
    fun analyzeSchema(@RequestBody request: SchemaAnalysisRequest): ResponseEntity<Any>
    
    @PostMapping("/validate-queries")
    fun validateQueries(@RequestBody request: QueryValidationRequest): ResponseEntity<Any>
}
```

#### Пример 3: MCP Server для API Testing

```kotlin
// Domain Service
@Service
class ApiTestingService(
    private val properties: ApiTestingProperties,
    private val outputParser: ApiTestOutputParser
) {
    fun testApiEndpoint(url: String, method: String, headers: Map<String, String>): ApiTestResult {
        // Выполнение HTTP запросов, проверка ответов
    }
    
    fun generateTestSuite(apiSpec: String, framework: String): TestSuiteResult {
        // Генерация тестов на основе OpenAPI/Swagger спецификации
    }
}

// DTOs
data class ApiTestRequest(
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val expectedStatus: Int? = null
)

data class ApiTestResponse(
    val status: Int,
    val responseTime: Long,
    val headers: Map<String, String>,
    val body: String,
    val passed: Boolean
)

// Controller
@RestController
@RequestMapping("/api")
class ApiTestingController(
    private val apiTestingService: ApiTestingService
) {
    @PostMapping("/test-endpoint")
    fun testEndpoint(@RequestBody request: ApiTestRequest): ResponseEntity<Any>
    
    @PostMapping("/generate-tests")
    fun generateTests(@RequestBody request: TestGenerationRequest): ResponseEntity<Any>
}
```

### 2.4 Шаги по созданию нового MCP-сервера

#### Шаг 1: Клонирование шаблона
```bash
# Клонируйте репозиторий MCP BSL Server
git clone https://github.com/yourusername/mcp-bsl-server.git your-new-mcp-server
cd your-new-mcp-server

# Удалите Git историю
rm -rf .git
git init
```

#### Шаг 2: Переименование пакетов
```bash
# Замените все вхождения пакета
find . -type f -name "*.kt" -exec sed -i 's/com.github.steel33ff.mcpbsl/com.yourcompany.yourdomain/g' {} +
find . -type f -name "*.yml" -exec sed -i 's/mcp-bsl-server/your-new-server/g' {} +
find . -type f -name "*.xml" -exec sed -i 's/mcp-bsl-server/your-new-server/g' {} +
```

#### Шаг 3: Обновление build.gradle.kts
```kotlin
group = "com.yourcompany"
version = "0.1.0-SNAPSHOT"

// Обновите название приложения
springBoot {
    mainClass.set("com.yourcompany.yourdomain.YourMcpServerApplicationKt")
}
```

#### Шаг 4: Замена доменных компонентов
1. Удалите BSL-специфичные файлы:
   - `src/main/kotlin/.../bsl/` (кроме общих утилит)
   - `src/main/kotlin/.../controller/BslApiController.kt`
   - `src/main/kotlin/.../dto/ApiModels.kt` (BSL-специфичные части)

2. Создайте новые доменные компоненты:
   - Ваш Domain Service
   - Ваши DTO модели
   - Ваш API Controller
   - Ваши Configuration Properties

#### Шаг 5: Обновление Dockerfile
```dockerfile
# Замените BSL Language Server на ваш инструмент
ARG YOUR_TOOL_VERSION=latest
ARG YOUR_TOOL_PATH=/opt/your-tool/executable

RUN mkdir -p /opt/your-tool && \
    # Скачайте ваш инструмент
    wget -O "$YOUR_TOOL_PATH" "https://your-tool-url" && \
    chmod +x "$YOUR_TOOL_PATH"

# Обновите environment variables
ENV YOUR_TOOL_PATH=/opt/your-tool/executable
ENV YOUR_TOOL_TIMEOUT=300
```

#### Шаг 6: Обновление конфигурации
```yaml
# application.yml
yourdomain:
  tool-path: ${YOUR_TOOL_PATH:/opt/your-tool/executable}
  timeout: ${YOUR_TOOL_TIMEOUT:300}
  max-concurrency: ${YOUR_TOOL_MAX_CONCURRENCY:10}
```

#### Шаг 7: Обновление документации
1. Обновите `README.md` с описанием вашего сервера
2. Обновите `docs/ARCHITECTURE.md` с доменными особенностями
3. Создайте `docs/YOUR_DOMAIN_GUIDE.md` с инструкциями по использованию

### 2.5 Что НЕ нужно изменять

#### Инфраструктурные компоненты (сохраняются):
- `PathMappingService` - для работы с путями в Docker
- `StatusController` - для health checks
- `SessionController` - для управления сессиями (если нужно)
- Все логирование и мониторинг
- Swagger конфигурация
- Docker Compose стек
- Gradle зависимости для инфраструктуры

#### Конфигурационные файлы (сохраняются):
- `logback-spring.xml`
- `docker-compose.yml`
- `grafana/` и `prometheus/` конфигурации
- `application.yml` (кроме доменных секций)

### 2.6 Тестирование нового сервера

#### Unit тесты
```kotlin
@ExtendWith(MockKExtension::class)
class YourDomainServiceTest {
    
    @MockK
    private lateinit var properties: YourDomainProperties
    
    @MockK
    private lateinit var outputParser: YourOutputParser
    
    private lateinit var service: YourDomainService
    
    @BeforeEach
    fun setup() {
        service = YourDomainService(properties, outputParser)
    }
    
    @Test
    fun `should process data successfully`() {
        // Тест вашей бизнес-логики
    }
}
```

#### Integration тесты
```kotlin
@SpringBootTest
@AutoConfigureTestDatabase
class YourDomainControllerIntegrationTest {
    
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @Test
    fun `should process data via API`() {
        // Тест API endpoints
    }
}
```

### 2.7 Развертывание

#### Локальная разработка
```bash
# Сборка
./gradlew build

# Запуск
./gradlew bootRun
```

#### Docker
```bash
# Сборка образа
docker build -t your-mcp-server:latest .

# Запуск
docker run --rm -p 9090:9090 -p 8080:8080 your-mcp-server:latest
```

#### Полный стек
```bash
# Запуск с мониторингом
docker-compose up -d

# Доступные адреса:
# - Web UI: http://localhost:9090
# - Grafana: http://localhost:3000
# - Prometheus: http://localhost:9091
```

## Заключение

Данная архитектура предоставляет мощный шаблон для создания MCP-серверов с полной инфраструктурой мониторинга, логирования и документации. Основные преимущества:

1. **Быстрая разработка** - инфраструктура готова, нужно только заменить доменную логику
2. **Production-ready** - логирование, мониторинг, health checks из коробки
3. **Масштабируемость** - поддержка множественных транспортов
4. **Документация** - Swagger UI автоматически генерируется
5. **Контейнеризация** - Docker + Docker Compose готовы к использованию

Следуя данному руководству, вы можете создать специализированный MCP-сервер для любого домена, сохранив всю мощь инфраструктуры оригинального проекта.
