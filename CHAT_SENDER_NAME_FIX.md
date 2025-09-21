# Исправление проблемы с пустым senderName

## Проблема
При отправке сообщений в чат поле `senderName` было пустым, хотя все остальные данные отображались корректно.

## Причины проблемы
1. **Несогласованность в названиях полей**: Код искал `firstName`/`lastName`, но API возвращал `first_name`/`last_name`
2. **Отсутствие fallback логики**: Если получение данных пользователя не удавалось, не было резервного способа
3. **Недостаточная проверка в convertToDTO**: Метод не проверял, что `senderName` не пустой

## Внесенные исправления

### 1. Улучшенная логика получения имени пользователя
```java
// Пробуем разные варианты полей
if (userInfo.get("firstName") != null) {
    firstName = userInfo.get("firstName").toString();
} else if (userInfo.get("first_name") != null) {
    firstName = userInfo.get("first_name").toString();
}

if (userInfo.get("lastName") != null) {
    lastName = userInfo.get("lastName").toString();
} else if (userInfo.get("last_name") != null) {
    lastName = userInfo.get("last_name").toString();
}
```

### 2. Fallback через getUserProfile
```java
// Если не удалось получить данные пользователя, попробуем через getUserProfile
try {
    ResponseEntity<Response> profileResponse = userService.getUserProfile(userId, language);
    // ... логика получения имени
} catch (Exception e) {
    log.warn("Failed to get user profile for ID {}: {}", userId, e.getMessage());
}
```

### 3. Защита в convertToDTO
```java
// Убеждаемся, что senderName не пустой
String senderName = message.getSenderName();
if (senderName == null || senderName.trim().isEmpty()) {
    if (message.getSenderId() != null && message.getSenderId() != 0) {
        senderName = "User " + message.getSenderId();
    } else {
        senderName = "System";
    }
}
```

### 4. Добавлено логирование для отладки
```java
log.info("User response for userId {}: status={}, body={}", userId, userResponse.getStatusCode(), userResponse.getBody());
log.info("User info for userId {}: {}", userId, userInfo);
log.info("Extracted name for userId {}: firstName='{}', lastName='{}', fullName='{}'", userId, firstName, lastName, fullName);
```

## Тестирование исправления

### 1. Отправка сообщения через WebSocket
```javascript
// HTML тест
const socket = new SockJS('http://localhost:8080/PlaceAndPlay/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({
    'Authorization': 'Bearer YOUR_ACCESS_TOKEN'
}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Подписка на чат
    stompClient.subscribe('/topic/chat/1276', function(message) {
        const messageData = JSON.parse(message.body);
        console.log('Received message:', messageData);
        console.log('Sender name:', messageData.senderName); // Должно быть заполнено
    });
});

// Отправка сообщения
stompClient.send('/app/chat.send/1276', {
    'userId': 6,
    'accessToken': 'YOUR_ACCESS_TOKEN',
    'refreshToken': 'YOUR_REFRESH_TOKEN',
    'language': 'ru'
}, JSON.stringify({
    content: "Тестовое сообщение",
    quotedMessageId: null,
    quotedMessageContent: null,
    quotedMessageSender: null
}));
```

### 2. Проверка логов
```bash
# Отслеживание логов при отправке сообщения
tail -f logs/application.log | grep -E "(User response|User info|Extracted name|senderName)"
```

### 3. Проверка в базе данных
```sql
-- Проверка сохраненных сообщений
SELECT message_id, sender_id, sender_name, content, sent_at 
FROM events.event_messages 
WHERE event_id = 1276 
ORDER BY sent_at DESC 
LIMIT 10;
```

## Ожидаемый результат

После исправления:
1. **senderName** должен быть заполнен реальным именем пользователя
2. Если имя не удается получить, используется fallback "User {userId}"
3. Системные сообщения имеют senderName = "System"
4. В логах видны детали получения данных пользователя

## Мониторинг

Для мониторинга работы исправления:
1. Проверяйте логи на наличие ошибок получения данных пользователя
2. Следите за полем senderName в WebSocket сообщениях
3. Проверяйте сохранение senderName в базе данных
4. Тестируйте с разными пользователями и токенами

## Дополнительные улучшения

В будущем можно добавить:
1. Кэширование имен пользователей
2. Асинхронное обновление имен пользователей
3. Валидацию токенов перед получением данных пользователя
4. Метрики для отслеживания успешности получения имен пользователей
