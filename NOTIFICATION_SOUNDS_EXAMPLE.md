# Примеры кастомных звуков уведомлений

## 🔊 Что реализовано

### 1. **Enum для типов звуков**
```java
// src/main/java/com/is/auth/model/NotificationSound.java
public enum NotificationSound {
    CHAT_MESSAGE("chat_message.wav", "Звук нового сообщения в чате"),
    SYSTEM_NOTIFICATION("system_notification.wav", "Звук системного уведомления"),
    NEW_EVENT("new_event.wav", "Звук нового события"),
    // ... другие звуки
}
```

### 2. **Обновленный PushNotificationService**
```java
// Для сообщений чата
NotificationSound sound = NotificationSound.getSoundForNotificationType("NEW_CHAT_MESSAGE");

Message message = Message.builder()
    .setToken(token.getToken())
    .setNotification(Notification.builder()
        .setTitle(title)
        .setBody(body)
        .build())
    .putData("sound", sound.getFileName()) // Кастомный звук
    .putData("soundType", sound.name()) // Тип звука
    .build();
```

## 📱 Настройка в Flutter

### 1. **Структура файлов**
```
android/app/src/main/res/raw/
├── chat_message.wav
├── system_notification.wav
├── new_event.wav
└── default.wav

ios/Runner/
├── chat_message.wav
├── system_notification.wav
├── new_event.wav
└── default.wav
```

### 2. **Обработка в Flutter**
```dart
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class NotificationService {
  static Future<void> showNotification({
    required String title,
    required String body,
    String? soundFile,
    String? soundType,
    Map<String, dynamic>? data,
  }) async {
    final flutterLocalNotificationsPlugin = FlutterLocalNotificationsPlugin();
    
    // Определяем звук
    String? sound;
    if (soundFile != null) {
      sound = soundFile;
    } else if (soundType != null) {
      sound = _getSoundForType(soundType);
    }
    
    await flutterLocalNotificationsPlugin.show(
      DateTime.now().millisecondsSinceEpoch ~/ 1000,
      title,
      body,
      NotificationDetails(
        android: AndroidNotificationDetails(
          'placeandplay_notifications',
          'PlaceAndPlay Notifications',
          channelDescription: 'Уведомления PlaceAndPlay',
          importance: Importance.high,
          priority: Priority.high,
          sound: sound != null ? RawResourceAndroidNotificationSound(sound) : null,
        ),
        iOS: DarwinNotificationDetails(
          sound: sound != null ? '$sound.wav' : null,
        ),
      ),
      payload: data != null ? jsonEncode(data) : null,
    );
  }

  static String? _getSoundForType(String soundType) {
    switch (soundType) {
      case 'CHAT_MESSAGE':
        return 'chat_message';
      case 'SYSTEM_NOTIFICATION':
        return 'system_notification';
      case 'NEW_EVENT':
        return 'new_event';
      default:
        return 'default';
    }
  }
}
```

### 3. **Обработка FCM**
```dart
import 'package:firebase_messaging/firebase_messaging.dart';

FirebaseMessaging.onMessage.listen((RemoteMessage message) {
  print('Received message: ${message.messageId}');
  
  // Показываем уведомление с кастомным звуком
  NotificationService.showNotification(
    title: message.notification?.title ?? 'Уведомление',
    body: message.notification?.body ?? '',
    soundFile: message.data['sound'],
    soundType: message.data['soundType'],
    data: message.data,
  );
});
```

## 🎵 Создание звуковых файлов

### Требования
- **Формат**: WAV, MP3, OGG
- **Длительность**: 1-3 секунды
- **Качество**: 44.1 kHz, 16-bit
- **Размер**: до 1 MB

### Рекомендации
- Короткие, узнаваемые звуки
- Умеренная громкость
- Тестирование на разных устройствах

## 🧪 Тестирование

### 1. **Тест через WebSocket**
```javascript
// Отправка сообщения с кастомным звуком
const message = {
  destination: '/app/chat.send/1276',
  headers: {
    userId: 6,
    accessToken: 'YOUR_TOKEN',
    refreshToken: 'YOUR_REFRESH_TOKEN',
    language: 'ru'
  },
  body: JSON.stringify({
    content: "Тест кастомного звука! 🔊",
    quotedMessageId: null
  })
};

ws.send(JSON.stringify(message));
```

### 2. **Проверка payload**
```json
{
  "type": "NEW_CHAT_MESSAGE",
  "eventId": "1276",
  "senderId": "6",
  "senderName": "Иван Иванов",
  "messageText": "Тест кастомного звука! 🔊",
  "sound": "chat_message.wav",
  "soundType": "CHAT_MESSAGE",
  "click_action": "FLUTTER_NOTIFICATION_CLICK"
}
```

### 3. **Тест в Flutter**
```dart
// Тестирование разных звуков
void testSounds() {
  final sounds = ['chat_message', 'system_notification', 'new_event', 'default'];
  
  for (int i = 0; i < sounds.length; i++) {
    Future.delayed(Duration(seconds: i * 2), () {
      NotificationService.showNotification(
        title: 'Тест звука ${i + 1}',
        body: 'Звук: ${sounds[i]}',
        soundFile: sounds[i],
      );
    });
  }
}
```

## 🔧 Настройки пользователя

### 1. **Класс настроек**
```dart
class SoundSettings {
  bool chatSoundsEnabled = true;
  bool eventSoundsEnabled = true;
  bool systemSoundsEnabled = true;
  String defaultSound = 'default';
  
  Future<void> saveSettings() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('chat_sounds_enabled', chatSoundsEnabled);
    await prefs.setBool('event_sounds_enabled', eventSoundsEnabled);
    await prefs.setBool('system_sounds_enabled', systemSoundsEnabled);
    await prefs.setString('default_sound', defaultSound);
  }
}
```

### 2. **UI настроек**
```dart
class SoundSettingsPage extends StatefulWidget {
  @override
  _SoundSettingsPageState createState() => _SoundSettingsPageState();
}

class _SoundSettingsPageState extends State<SoundSettingsPage> {
  SoundSettings settings = SoundSettings();
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Настройки звуков')),
      body: ListView(
        children: [
          SwitchListTile(
            title: Text('Звуки чата'),
            value: settings.chatSoundsEnabled,
            onChanged: (value) {
              setState(() {
                settings.chatSoundsEnabled = value;
              });
              settings.saveSettings();
            },
          ),
          SwitchListTile(
            title: Text('Звуки событий'),
            value: settings.eventSoundsEnabled,
            onChanged: (value) {
              setState(() {
                settings.eventSoundsEnabled = value;
              });
              settings.saveSettings();
            },
          ),
          SwitchListTile(
            title: Text('Системные звуки'),
            value: settings.systemSoundsEnabled,
            onChanged: (value) {
              setState(() {
                settings.systemSoundsEnabled = value;
              });
              settings.saveSettings();
            },
          ),
        ],
      ),
    );
  }
}
```

## 📋 Чек-лист внедрения

- [x] Создан enum NotificationSound
- [x] Обновлен PushNotificationService
- [x] Добавлена поддержка звуков в FCM payload
- [ ] Создать звуковые файлы
- [ ] Добавить файлы в Android проект
- [ ] Добавить файлы в iOS проект
- [ ] Реализовать обработку в Flutter
- [ ] Добавить настройки звуков
- [ ] Протестировать на устройствах

## 🚀 Следующие шаги

1. **Создать звуковые файлы** для всех типов уведомлений
2. **Добавить файлы в проекты** Android и iOS
3. **Реализовать обработку** в Flutter приложении
4. **Добавить настройки** для пользователей
5. **Протестировать** на реальных устройствах

Теперь у вас есть полная система кастомных звуков! 🎵
