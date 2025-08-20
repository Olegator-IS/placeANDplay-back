# SFTP Setup and Configuration

## Обзор

Система теперь использует SFTP (SSH File Transfer Protocol) для загрузки файлов на сервер. SFTP обеспечивает безопасную передачу файлов через SSH соединение.

## Конфигурация

### 1. Настройки в application.properties

```properties
# SFTP Configuration
sftp.server=95.46.96.94
sftp.port=22
sftp.username=root
sftp.password=Testpassword

# File upload configuration
app.upload.dir=/home/placeand/uploads
app.domain=https://placeandplay.uz
```

### 2. Зависимости

В `pom.xml` уже добавлена зависимость JSch:

```xml
<dependency>
    <groupId>com.jcraft</groupId>
    <artifactId>jsch</artifactId>
    <version>0.1.55</version>
</dependency>
```

## Компоненты

### 1. SftpService

Основной сервис для работы с SFTP:

- **uploadFile()** - загрузка файла на SFTP сервер
- **testConnection()** - тестирование соединения
- **createDirectories()** - создание директорий на сервере

### 2. FileStorageService

Обновлен для использования SFTP вместо FTP:

- Генерирует уникальные имена файлов
- Формирует пути для загрузки
- Возвращает URL для доступа к файлу

### 3. Эндпоинт тестирования

`GET /api/auth/test-sftp` - для проверки SFTP соединения

## Использование

### 1. Загрузка файла

```java
// Автоматически вызывается при загрузке профильной картинки
@PostMapping("/upload-profile-picture")
public ResponseEntity<Response> uploadProfilePicture(
    @RequestParam("file") MultipartFile file,
    @RequestParam("userId") Long userId,
    @RequestHeader(value = "language", defaultValue = "ru") String language) {
    return userService.uploadProfilePicture(file, userId);
}
```

### 2. Тестирование соединения

```bash
curl -X GET "http://localhost:8080/PlaceAndPlay/api/auth/test-sftp"
```

**Успешный ответ:**
```json
{
    "status": "UP",
    "message": "SFTP connection successful",
    "timestamp": "2024-01-01T12:00:00"
}
```

**Ошибка:**
```json
{
    "status": "DOWN",
    "message": "SFTP connection failed",
    "timestamp": "2024-01-01T12:00:00"
}
```

## Логирование

SFTP сервис предоставляет подробное логирование:

```
INFO  - Connecting to SFTP server: 95.46.96.94:22
INFO  - Logging in to SFTP server with username: root
INFO  - Creating directories for path: /home/placeand/uploads/profile-pictures
INFO  - Created directory: /home/placeand/uploads
INFO  - Created directory: /home/placeand/uploads/profile-pictures
INFO  - Uploading file to: /home/placeand/uploads/profile-pictures/uuid_filename.jpg
INFO  - File uploaded successfully to SFTP: /home/placeand/uploads/profile-pictures/uuid_filename.jpg
INFO  - SFTP connection closed
```

## Безопасность

### 1. SSH ключи (рекомендуется)

Для продакшена рекомендуется использовать SSH ключи вместо паролей:

```java
// В SftpService можно добавить поддержку ключей
jsch.addIdentity("path/to/private/key");
session = jsch.getSession(username, server, port);
// Убираем session.setPassword(password);
```

### 2. Настройки безопасности

```java
// Отключаем проверку known_hosts для разработки
java.util.Properties config = new java.util.Properties();
config.put("StrictHostKeyChecking", "no");
session.setConfig(config);
```

## Устранение неполадок

### 1. Проблемы с соединением

- Проверьте доступность сервера: `ping 95.46.96.94`
- Проверьте SSH порт: `telnet 95.46.96.94 22`
- Убедитесь в правильности учетных данных

### 2. Проблемы с правами доступа

- Проверьте права на папку `/home/placeand/uploads`
- Убедитесь, что пользователь `root` имеет права на запись

### 3. Проблемы с директориями

- SFTP сервис автоматически создает необходимые директории
- Проверьте логи на наличие ошибок создания директорий

### 4. Проблемы с файлами

- Проверьте размер файла (максимум 5MB)
- Убедитесь в правильности формата файла
- Проверьте права на чтение временного файла

## Мониторинг

### 1. Health Check

Добавьте проверку SFTP в общий health check:

```java
@GetMapping("/health")
public ResponseEntity<Map<String, Object>> healthCheck() {
    Map<String, Object> healthStatus = new HashMap<>();
    try {
        // Проверяем SFTP соединение
        boolean sftpOk = sftpService.testConnection();
        healthStatus.put("sftp", sftpOk ? "UP" : "DOWN");
        
        // ... остальные проверки
        
        return ResponseEntity.ok(healthStatus);
    } catch (Exception e) {
        healthStatus.put("sftp", "DOWN");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(healthStatus);
    }
}
```

### 2. Метрики

Можно добавить метрики для мониторинга:

- Количество успешных загрузок
- Количество ошибок загрузки
- Время загрузки файлов
- Размер загруженных файлов

## Производительность

### 1. Оптимизации

- Используйте пул соединений для частых загрузок
- Кэшируйте SSH сессии
- Используйте асинхронную загрузку для больших файлов

### 2. Ограничения

- Максимальный размер файла: 5MB
- Таймаут соединения: по умолчанию JSch
- Количество одновременных соединений: ограничено сервером

## Миграция с FTP

Если вы переходите с FTP на SFTP:

1. Обновите конфигурацию в `application.properties`
2. Замените `FtpService` на `SftpService`
3. Обновите все зависимости в контроллерах
4. Протестируйте соединение через `/test-sftp`
5. Проверьте загрузку файлов
