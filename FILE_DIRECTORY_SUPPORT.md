# Поддержка файлов и директорий в BSL Language Server

## Обзор

BSL Language Server теперь поддерживает работу как с отдельными файлами, так и с директориями. Это позволяет анализировать и форматировать как отдельные BSL файлы, так и целые проекты.

## Новые возможности

### 1. Анализ файлов и директорий

API endpoint `/api/analyze` теперь принимает:
- **Файлы**: `.bsl`, `.os` файлы
- **Директории**: папки, содержащие BSL файлы

```json
{
  "srcDir": "D:\\Projects\\MyProject\\Module.bsl",  // или директория
  "reporters": ["json"],
  "language": "ru"
}
```

### 2. Форматирование файлов и директорий

API endpoint `/api/format` поддерживает:
- Форматирование отдельного файла
- Форматирование всех BSL файлов в директории

```json
{
  "src": "D:\\Projects\\MyProject\\Module.bsl",  // или директория
  "inPlace": true
}
```

### 3. Информация о пути

Новый endpoint `/api/path-info` предоставляет информацию о типе пути:

```bash
GET /api/path-info?path=D:\Projects\MyProject\Module.bsl
```

Ответ:
```json
{
  "type": "BSL_FILE",
  "exists": true,
  "isBsl": true,
  "sizeBytes": 1024,
  "bslFileCount": null,
  "containerPath": "/workspaces/MyProject/Module.bsl"
}
```

## Типы путей

### PathType enum

- `BSL_FILE` - BSL файл (.bsl, .os)
- `BSL_DIRECTORY` - директория, содержащая BSL файлы
- `FILE` - обычный файл (не BSL)
- `DIRECTORY` - обычная директория (без BSL файлов)
- `NOT_FOUND` - путь не существует
- `UNKNOWN` - неизвестный или недоступный путь

## Автоматическое определение типа

Система автоматически определяет тип пути и использует соответствующие параметры BSL Language Server:

- Для файлов: `--src <путь_к_файлу>`
- Для директорий: `--srcDir <путь_к_директории>`

## Валидация путей

Все пути проходят валидацию:
- Проверка существования
- Проверка доступности
- Проверка безопасности (в пределах разрешенной директории)

## Примеры использования

### Анализ отдельного файла

```bash
curl -X POST http://localhost:9090/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "srcDir": "D:\\Projects\\MyProject\\Module.bsl",
    "reporters": ["json"],
    "language": "ru"
  }'
```

### Анализ директории

```bash
curl -X POST http://localhost:9090/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "srcDir": "D:\\Projects\\MyProject",
    "reporters": ["json"],
    "language": "ru"
  }'
```

### Получение информации о пути

```bash
curl "http://localhost:9090/api/path-info?path=D:\\Projects\\MyProject"
```

### Форматирование файла

```bash
curl -X POST http://localhost:9090/api/format \
  -H "Content-Type: application/json" \
  -d '{
    "src": "D:\\Projects\\MyProject\\Module.bsl",
    "inPlace": true
  }'
```

## Обработка ошибок

### PATH_NOT_FOUND

Возвращается когда:
- Путь не существует
- Путь недоступен
- Путь находится вне разрешенной директории

```json
{
  "error": "Path 'D:\\nonexistent\\path.bsl' does not exist or is not accessible",
  "code": "PATH_NOT_FOUND"
}
```

## Тестирование

Созданы comprehensive тесты:

- `PathTypeServiceTest` - unit тесты для определения типов путей
- `BslApiControllerIntegrationTest` - интеграционные тесты для API

Запуск тестов:
```bash
./gradlew test --tests "*PathTypeServiceTest*"
./gradlew test --tests "*BslApiControllerIntegrationTest*"
```

## Обратная совместимость

Все изменения обратно совместимы:
- Существующие API endpoints работают без изменений
- Параметры запросов остались те же
- Ответы API не изменились

## Архитектура

### Новые компоненты

1. **PathTypeService** - определение типов путей и BSL файлов
2. **PathInfoResponse** - DTO для информации о пути
3. **PathType** - enum типов путей

### Обновленные компоненты

1. **BslCliService** - поддержка файлов и директорий
2. **PathMappingService** - валидация файлов и директорий
3. **BslApiController** - новый endpoint и валидация
4. **StreamingController** - обновлен для нового API

## Конфигурация

Никаких дополнительных настроек не требуется. Система автоматически определяет тип пути и использует соответствующие параметры BSL Language Server.
