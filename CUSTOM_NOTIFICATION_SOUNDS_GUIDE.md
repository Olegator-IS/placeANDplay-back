# Руководство по кастомным звукам уведомлений

## 🔊 Обзор

Система поддерживает кастомные звуки для разных типов уведомлений. Звуки настраиваются как на сервере (Firebase FCM), так и на клиенте (Flutter).

## 📁 Структура звуковых файлов

### Android (Flutter)
```
android/app/src/main/res/raw/
├── chat_message.wav          # Звук сообщений чата
├── system_notification.wav   # Звук системных уведомлений
├── new_event.wav            # Звук новых событий
├── event_confirmed.wav      # Звук подтверждения события
├── event_cancelled.wav      # Звук отмены события
├── event_started.wav        # Звук начала события
├── participant_joined.wav   # Звук присоединения участника
├── participant_left.wav     # Звук выхода участника
├── friend_request.wav       # Звук запроса дружбы
├── friend_accepted.wav      # Звук принятия дружбы
├── default.wav              # Стандартный звук
├── silent.wav               # Тихий звук
└── vibration_only.wav       # Только вибрация
```

### iOS (Flutter)
```
ios/Runner/
├── chat_message.wav
├── system_notification.wav
├── new_event.wav
├── event_confirmed.wav
├── event_cancelled.wav
├── event_started.wav
├── participant_joined.wav
├── participant_left.wav
├── friend_request.wav
├── friend_accepted.wav
├── default.wav
├── silent.wav
└── vibration_only.wav
```

## 🎵 Типы звуков

### Чат
- **CHAT_MESSAGE**: `chat_message.wav` - Звук нового сообщения в чате
- **SYSTEM_NOTIFICATION**: `system_notification.wav` - Звук системного уведомления

### События
- **NEW_EVENT**: `new_event.wav` - Звук нового события
- **EVENT_CONFIRMED**: `event_confirmed.wav` - Звук подтверждения события
- **EVENT_CANCELLED**: `event_cancelled.wav` - Звук отмены события
- **EVENT_STARTED**: `event_started.wav` - Звук начала события

### Участники
- **PARTICIPANT_JOINED**: `participant_joined.wav` - Звук присоединения участника
- **PARTICIPANT_LEFT**: `participant_left.wav` - Звук выхода участника

### Дружба
- **FRIEND_REQUEST**: `friend_request.wav` - Звук запроса дружбы
- **FRIEND_ACCEPTED**: `friend_accepted.wav` - Звук принятия дружбы

### Общие
- **DEFAULT**: `default.wav` - Стандартный звук уведомления
- **SILENT**: `silent.wav` - Тихий звук
- **VIBRATION_ONLY**: `vibration_only.wav` - Только вибрация

## 🛠️ Настройка на сервере

### 1. Enum для типов звуков
```java
// src/main/java/com/is/auth/model/NotificationSound.java
public enum NotificationSound {
    CHAT_MESSAGE("chat_message.wav", "Звук нового сообщения в чате"),
    SYSTEM_NOTIFICATION("system_notification.wav", "Звук системного уведомления"),
    // ... другие звуки
}
```

### 2. Использование в PushNotificationService
```java
NotificationSound sound = NotificationSound.getSoundForNotificationType("NEW_CHAT_MESSAGE");

Message message = Message.builder()
    .setToken(token.getToken())
    .setNotification(Notification.builder()
        .setTitle(title)
        .setBody(body)
        .setSound(sound.getFileName()) // Кастомный звук
        .build())
    .putData("sound", sound.getFileName()) // Дублируем в data
    .putData("soundType", sound.name()) // Тип звука
    .build();
```

## 📱 Настройка в Flutter

### 1. Добавление звуковых файлов

#### Android
```bash
# Создайте папку для звуков
mkdir -p android/app/src/main/res/raw

# Скопируйте звуковые файлы
cp sounds/*.wav android/app/src/main/res/raw/
```

#### iOS
```bash
# Добавьте звуковые файлы в Xcode проект
# Или скопируйте в ios/Runner/
cp sounds/*.wav ios/Runner/
```

### 2. Обработка в Flutter
```dart
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class NotificationService {
  static const AndroidNotificationChannel channel = AndroidNotificationChannel(
    'placeandplay_notifications',
    'PlaceAndPlay Notifications',
    description: 'Уведомления PlaceAndPlay',
    importance: Importance.high,
    sound: RawResourceAndroidNotificationSound('default'),
  );

  static Future<void> initialize() async {
    final flutterLocalNotificationsPlugin = FlutterLocalNotificationsPlugin();
    
    await flutterLocalNotificationsPlugin.initialize(
      InitializationSettings(
        android: AndroidInitializationSettings('@mipmap/ic_launcher'),
        iOS: DarwinInitializationSettings(),
      ),
    );
  }

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
          channel.id,
          channel.name,
          channelDescription: channel.description,
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
      case 'EVENT_CONFIRMED':
        return 'event_confirmed';
      case 'EVENT_CANCELLED':
        return 'event_cancelled';
      case 'EVENT_STARTED':
        return 'event_started';
      case 'PARTICIPANT_JOINED':
        return 'participant_joined';
      case 'PARTICIPANT_LEFT':
        return 'participant_left';
      case 'FRIEND_REQUEST':
        return 'friend_request';
      case 'FRIEND_ACCEPTED':
        return 'friend_accepted';
      case 'SILENT':
        return 'silent';
      case 'VIBRATION_ONLY':
        return 'vibration_only';
      default:
        return 'default';
    }
  }
}
```

### 3. Обработка FCM уведомлений
```dart
import 'package:firebase_messaging/firebase_messaging.dart';

class FCMService {
  static Future<void> initialize() async {
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      print('Received message: ${message.messageId}');
      
      // Показываем локальное уведомление с кастомным звуком
      NotificationService.showNotification(
        title: message.notification?.title ?? 'Уведомление',
        body: message.notification?.body ?? '',
        soundFile: message.data['sound'],
        soundType: message.data['soundType'],
        data: message.data,
      );
    });
  }
}
```

## 🎨 Создание звуковых файлов

### Требования к звукам
- **Формат**: WAV, MP3, OGG
- **Длительность**: 1-3 секунды
- **Качество**: 44.1 kHz, 16-bit
- **Размер**: до 1 MB
- **Громкость**: умеренная (не слишком громко)

### Рекомендации
- Используйте короткие, узнаваемые звуки
- Избегайте резких или раздражающих звуков
- Тестируйте звуки на разных устройствах
- Учитывайте настройки пользователя (тихий режим, вибрация)

### Инструменты для создания
- **Audacity** (бесплатный)
- **GarageBand** (Mac)
- **Adobe Audition** (платный)
- **Online звуковые генераторы**

## 🔧 Настройка пользователя

### 1. Настройки звуков в приложении
```dart
class SoundSettings {
  bool chatSoundsEnabled = true;
  bool eventSoundsEnabled = true;
  bool systemSoundsEnabled = true;
  String defaultSound = 'default';
  
  // Сохранение настроек
  Future<void> saveSettings() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('chat_sounds_enabled', chatSoundsEnabled);
    await prefs.setBool('event_sounds_enabled', eventSoundsEnabled);
    await prefs.setBool('system_sounds_enabled', systemSoundsEnabled);
    await prefs.setString('default_sound', defaultSound);
  }
  
  // Загрузка настроек
  Future<void> loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    chatSoundsEnabled = prefs.getBool('chat_sounds_enabled') ?? true;
    eventSoundsEnabled = prefs.getBool('event_sounds_enabled') ?? true;
    systemSoundsEnabled = prefs.getBool('system_sounds_enabled') ?? true;
    defaultSound = prefs.getString('default_sound') ?? 'default';
  }
}
```

### 2. UI для настроек
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
          ListTile(
            title: Text('Стандартный звук'),
            subtitle: Text(settings.defaultSound),
            onTap: () {
              _showSoundPicker();
            },
          ),
        ],
      ),
    );
  }
  
  void _showSoundPicker() {
    showModalBottomSheet(
      context: context,
      builder: (context) => SoundPicker(
        onSoundSelected: (sound) {
          setState(() {
            settings.defaultSound = sound;
          });
          settings.saveSettings();
        },
      ),
    );
  }
}
```

## 🧪 Тестирование

### 1. Тест звуков на сервере
```bash
# Отправка тестового уведомления
curl -X POST http://localhost:8080/PlaceAndPlay/api/test/notification \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "type": "NEW_CHAT_MESSAGE",
    "title": "Тест звука",
    "body": "Проверка кастомного звука",
    "sound": "chat_message.wav"
  }'
```

### 2. Тест в Flutter
```dart
// Тестирование разных звуков
void testSounds() {
  final sounds = [
    'chat_message',
    'system_notification',
    'new_event',
    'default',
    'silent'
  ];
  
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

## 📋 Чек-лист внедрения

- [ ] Создать звуковые файлы для всех типов уведомлений
- [ ] Добавить звуковые файлы в Android проект
- [ ] Добавить звуковые файлы в iOS проект
- [ ] Обновить PushNotificationService для использования звуков
- [ ] Реализовать обработку звуков в Flutter
- [ ] Добавить настройки звуков в приложение
- [ ] Протестировать звуки на разных устройствах
- [ ] Обновить документацию

## 🚨 Важные замечания

1. **Размер файлов**: Звуковые файлы увеличивают размер приложения
2. **Производительность**: Слишком много звуков может замедлить приложение
3. **Пользовательский опыт**: Учитывайте предпочтения пользователей
4. **Доступность**: Предоставляйте альтернативы для пользователей с нарушениями слуха
5. **Батарея**: Частые звуки могут разряжать батарею

Теперь у вас есть полная система кастомных звуков для уведомлений! 🎵
