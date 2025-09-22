# Руководство по уведомлениям чата

## Обзор

Система уведомлений чата в PlaceAndPlay автоматически отправляет push-уведомления участникам события при отправке сообщений в чат.

## Типы уведомлений

### 1. Обычные сообщения пользователей
- **Тип**: `NEW_CHAT_MESSAGE`
- **Триггер**: Когда пользователь отправляет сообщение в чат события
- **Получатели**: Все участники события, кроме отправителя
- **Заголовок**: `💬 Новое сообщение в "{название спорта}"`
- **Текст**: `{имя отправителя}: {текст сообщения}`

### 2. Системные сообщения
- **Тип**: `SYSTEM_CHAT_MESSAGE`
- **Триггер**: Когда система отправляет сообщение (userId = 0)
- **Получатели**: Все участники события
- **Заголовок**: `🔔 Уведомление в "{название спорта}"`
- **Текст**: Текст системного сообщения

## Структура данных уведомления

### Firebase FCM Payload
```json
{
  "type": "NEW_CHAT_MESSAGE" | "SYSTEM_CHAT_MESSAGE",
  "eventId": "123",
  "senderId": "456",
  "senderName": "Иван Иванов",
  "messageText": "Привет всем!",
  "sportName": "Футбол",
  "click_action": "FLUTTER_NOTIFICATION_CLICK",
  "deepLink": "placeandplay://chat/123"
}
```

### База данных (notifications table)
```json
{
  "userId": 789,
  "type": "NEW_CHAT_MESSAGE",
  "title": "💬 Новое сообщение в \"Футбол\"",
  "message": "Иван Иванов: Привет всем!",
  "payload": {
    "eventId": 123,
    "senderId": 456,
    "senderName": "Иван Иванов",
    "messageText": "Привет всем!",
    "sportName": "Футбол"
  }
}
```

## Настройка Flutter приложения

### 1. Обработка уведомлений
```dart
FirebaseMessaging.onMessage.listen((RemoteMessage message) {
  if (message.data['type'] == 'NEW_CHAT_MESSAGE' || 
      message.data['type'] == 'SYSTEM_CHAT_MESSAGE') {
    // Показать уведомление
    _showChatNotification(message);
  }
});
```

### 2. Обработка клика по уведомлению
```dart
FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
  if (message.data['type'] == 'NEW_CHAT_MESSAGE' || 
      message.data['type'] == 'SYSTEM_CHAT_MESSAGE') {
    // Перейти к чату события
    final eventId = int.parse(message.data['eventId']);
    Navigator.pushNamed(context, '/chat', arguments: eventId);
  }
});
```

### 3. Deep Link обработка
```dart
// Обработка deep link: placeandplay://chat/123
void handleDeepLink(String link) {
  final uri = Uri.parse(link);
  if (uri.scheme == 'placeandplay' && uri.host == 'chat') {
    final eventId = int.parse(uri.pathSegments.first);
    Navigator.pushNamed(context, '/chat', arguments: eventId);
  }
}
```

## API Endpoints

### Отправка сообщения
```
POST /api/chat/send/{eventId}
WebSocket: /chat.send/{eventId}
```

### Получение уведомлений пользователя
```
GET /api/v1/notifications
Headers: userId: {userId}
```

### Отметить уведомление как прочитанное
```
POST /api/v1/notifications/{notificationId}/read
```

### Отметить все уведомления как прочитанные
```
GET /api/v1/notifications/readAll?userId={userId}
```

## Логирование

Все уведомления логируются с уровнем INFO:
```
Successfully sent chat message notification: {response}
Successfully sent system chat message notification: {response}
```

Ошибки логируются с уровнем ERROR:
```
Error sending chat message notification: {error}
Error sending system chat message notification: {error}
```

## Требования

1. **Firebase FCM** - настроен и работает
2. **FCM токены пользователей** - сохранены в базе данных
3. **WebSocket соединения** - для real-time обновлений
4. **Участники события** - определены в current_participants

## Безопасность

- Уведомления отправляются только участникам события
- Отправитель не получает уведомление о своем сообщении
- Системные сообщения отправляются всем участникам
- Все уведомления сохраняются в базе данных для истории

## Мониторинг

Для мониторинга работы уведомлений проверяйте:
1. Логи приложения на наличие ошибок отправки
2. Статистику Firebase Console
3. Количество уведомлений в базе данных
4. Активность WebSocket соединений
