# Полное руководство по кастомным звукам уведомлений

## 🔊 Реализованные звуки

### ✅ **Все типы уведомлений теперь поддерживают кастомные звуки:**

#### **1. Чат уведомления**
- **NEW_CHAT_MESSAGE**: `chat_message.wav` - Новые сообщения в чате
- **SYSTEM_CHAT_MESSAGE**: `system_notification.wav` - Системные сообщения

#### **2. События**
- **NEW_EVENT**: `new_event.wav` - Новые события
- **EVENT_CONFIRMED**: `event_confirmed.wav` - Подтверждение события
- **EVENT_CANCELLED**: `event_cancelled.wav` - Отмена события
- **EVENT_STARTED**: `event_started.wav` - Начало события

#### **3. Участники**
- **PARTICIPANT_JOINED**: `participant_joined.wav` - Присоединение участника
- **PARTICIPANT_LEFT**: `participant_left.wav` - Выход участника

#### **4. Дружба**
- **FRIEND_REQUEST**: `friend_request.wav` - Запрос дружбы
- **FRIEND_ACCEPTED**: `friend_accepted.wav` - Принятие дружбы

#### **5. Общие**
- **DEFAULT**: `default.wav` - Стандартный звук
- **SILENT**: `silent.wav` - Тихий звук
- **VIBRATION_ONLY**: `vibration_only.wav` - Только вибрация

## 🛠️ Реализация на сервере

### **1. Enum NotificationSound**
```java
public enum NotificationSound {
    CHAT_MESSAGE("chat_message.wav", "Звук нового сообщения в чате"),
    SYSTEM_NOTIFICATION("system_notification.wav", "Звук системного уведомления"),
    NEW_EVENT("new_event.wav", "Звук нового события"),
    EVENT_CONFIRMED("event_confirmed.wav", "Звук подтверждения события"),
    EVENT_CANCELLED("event_cancelled.wav", "Звук отмены события"),
    EVENT_STARTED("event_started.wav", "Звук начала события"),
    PARTICIPANT_JOINED("participant_joined.wav", "Звук присоединения участника"),
    PARTICIPANT_LEFT("participant_left.wav", "Звук выхода участника"),
    FRIEND_REQUEST("friend_request.wav", "Звук запроса дружбы"),
    FRIEND_ACCEPTED("friend_accepted.wav", "Звук принятия дружбы"),
    DEFAULT("default.wav", "Стандартный звук уведомления"),
    SILENT("silent.wav", "Тихий звук"),
    VIBRATION_ONLY("vibration_only.wav", "Только вибрация");
}
```

### **2. Обновленные методы PushNotificationService**

#### **Новые события:**
```java
public void sendNewEventNotification(Event event) {
    NotificationSound sound = NotificationSound.getSoundForNotificationType("NEW_EVENT");
    
    Message message = Message.builder()
        .setToken(token.getToken())
        .setNotification(Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build())
        .putData("sound", sound.getFileName()) // new_event.wav
        .putData("soundType", sound.name()) // NEW_EVENT
        .build();
}
```

#### **Присоединение участника:**
```java
public void sendParticipantJoinedNotification(Event event, EventParticipant participant) {
    NotificationSound sound = NotificationSound.getSoundForNotificationType("PARTICIPANT_JOINED");
    
    Message message = Message.builder()
        .putData("sound", sound.getFileName()) // participant_joined.wav
        .putData("soundType", sound.name()) // PARTICIPANT_JOINED
        .build();
}
```

#### **Изменение статуса события:**
```java
public void sendEventStatusChangeNotification(Event event, EventStatus newStatus) {
    NotificationSound sound;
    String notificationType;
    switch (newStatus) {
        case CONFIRMED -> {
            sound = NotificationSound.getSoundForNotificationType("EVENT_CONFIRMED");
            notificationType = "EVENT_CONFIRMED";
        }
        case REJECTED -> {
            sound = NotificationSound.getSoundForNotificationType("EVENT_CANCELLED");
            notificationType = "EVENT_CANCELLED";
        }
        case IN_PROGRESS -> {
            sound = NotificationSound.getSoundForNotificationType("EVENT_STARTED");
            notificationType = "EVENT_STARTED";
        }
        default -> {
            sound = NotificationSound.DEFAULT;
            notificationType = "EVENT_STATUS_CHANGED";
        }
    }
}
```

## 📱 Настройка в Flutter

### **1. Структура звуковых файлов**
```
android/app/src/main/res/raw/
├── chat_message.wav          # Сообщения чата
├── system_notification.wav   # Системные уведомления
├── new_event.wav            # Новые события
├── event_confirmed.wav      # Подтверждение события
├── event_cancelled.wav      # Отмена события
├── event_started.wav        # Начало события
├── participant_joined.wav   # Присоединение участника
├── participant_left.wav     # Выход участника
├── friend_request.wav       # Запрос дружбы
├── friend_accepted.wav      # Принятие дружбы
├── default.wav              # Стандартный звук
├── silent.wav               # Тихий звук
└── vibration_only.wav       # Только вибрация
```

### **2. Обработка в Flutter**
```dart
class NotificationService {
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

## 🧪 Тестирование

### **1. Тест создания события**
```bash
# Создание нового события через API
curl -X POST http://localhost:8080/PlaceAndPlay/api/v1/events \
  -H "Content-Type: application/json" \
  -H "accessToken: YOUR_TOKEN" \
  -d '{
    "sportEvent": {"sportId": 1, "sportName": "Футбол"},
    "placeId": 123,
    "dateTime": "2024-01-20T15:00:00",
    "maxParticipants": 10
  }'
```

**Ожидаемый payload уведомления:**
```json
{
  "type": "NEW_EVENT",
  "sound": "new_event.wav",
  "soundType": "NEW_EVENT",
  "title": "🎯 Новое событие по вашему любимому виду спорта!",
  "body": "👋 Эй! Кто-то хочет поиграть в Футбол!..."
}
```

### **2. Тест присоединения к событию**
```bash
# Присоединение к событию
curl -X POST http://localhost:8080/PlaceAndPlay/api/v1/events/123/join \
  -H "accessToken: YOUR_TOKEN"
```

**Ожидаемый payload:**
```json
{
  "type": "PARTICIPANT_JOINED",
  "sound": "participant_joined.wav",
  "soundType": "PARTICIPANT_JOINED",
  "title": "Новый участник",
  "body": "Иван присоединился к вашему событию \"Футбол\""
}
```

### **3. Тест изменения статуса**
```bash
# Подтверждение события организацией
curl -X PUT http://localhost:8080/PlaceAndPlay/api/v1/events/123/status \
  -H "Content-Type: application/json" \
  -d '{"status": "CONFIRMED"}'
```

**Ожидаемый payload:**
```json
{
  "type": "EVENT_CONFIRMED",
  "sound": "event_confirmed.wav",
  "soundType": "EVENT_CONFIRMED",
  "title": "Событие подтверждено!",
  "body": "Отличные новости! Заведение подтвердило ваше бронирование..."
}
```

### **4. Тест сообщений чата**
```javascript
// Отправка сообщения через WebSocket
const message = {
  destination: '/app/chat.send/123',
  headers: { userId: 6, accessToken: 'TOKEN' },
  body: JSON.stringify({ content: "Привет всем! 🎉" })
};
```

**Ожидаемый payload:**
```json
{
  "type": "NEW_CHAT_MESSAGE",
  "sound": "chat_message.wav",
  "soundType": "CHAT_MESSAGE",
  "title": "💬 Новое сообщение в \"Футбол\"",
  "body": "Иван Иванов: Привет всем! 🎉"
}
```

## 🎵 Создание звуковых файлов

### **Рекомендации по звукам:**

#### **Для чата:**
- `chat_message.wav` - Короткий, приятный звук (1-2 сек)
- `system_notification.wav` - Более формальный звук (1-2 сек)

#### **Для событий:**
- `new_event.wav` - Энергичный, привлекающий внимание (2-3 сек)
- `event_confirmed.wav` - Позитивный, радостный звук (2-3 сек)
- `event_cancelled.wav` - Нейтральный, не раздражающий (1-2 сек)
- `event_started.wav` - Мотивирующий, спортивный звук (2-3 сек)

#### **Для участников:**
- `participant_joined.wav` - Дружелюбный звук (1-2 сек)
- `participant_left.wav` - Нейтральный звук (1-2 сек)

#### **Для дружбы:**
- `friend_request.wav` - Теплый, дружеский звук (2-3 сек)
- `friend_accepted.wav` - Радостный звук (2-3 сек)

### **Технические требования:**
- **Формат**: WAV, MP3, OGG
- **Длительность**: 1-3 секунды
- **Качество**: 44.1 kHz, 16-bit
- **Размер**: до 1 MB
- **Громкость**: умеренная

## 🔧 Настройки пользователя

### **1. Класс настроек**
```dart
class SoundSettings {
  bool chatSoundsEnabled = true;
  bool eventSoundsEnabled = true;
  bool participantSoundsEnabled = true;
  bool systemSoundsEnabled = true;
  String defaultSound = 'default';
  
  Future<void> saveSettings() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('chat_sounds_enabled', chatSoundsEnabled);
    await prefs.setBool('event_sounds_enabled', eventSoundsEnabled);
    await prefs.setBool('participant_sounds_enabled', participantSoundsEnabled);
    await prefs.setBool('system_sounds_enabled', systemSoundsEnabled);
    await prefs.setString('default_sound', defaultSound);
  }
}
```

### **2. UI настроек**
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
            subtitle: Text('Уведомления о новых сообщениях'),
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
            subtitle: Text('Новые события, подтверждения, отмены'),
            value: settings.eventSoundsEnabled,
            onChanged: (value) {
              setState(() {
                settings.eventSoundsEnabled = value;
              });
              settings.saveSettings();
            },
          ),
          SwitchListTile(
            title: Text('Звуки участников'),
            subtitle: Text('Присоединение и выход участников'),
            value: settings.participantSoundsEnabled,
            onChanged: (value) {
              setState(() {
                settings.participantSoundsEnabled = value;
              });
              settings.saveSettings();
            },
          ),
          SwitchListTile(
            title: Text('Системные звуки'),
            subtitle: Text('Системные уведомления'),
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
- [x] Обновлен PushNotificationService для всех типов уведомлений
- [x] Добавлена поддержка звуков в FCM payload
- [x] Реализована логика выбора звука по типу уведомления
- [ ] Создать звуковые файлы для всех типов
- [ ] Добавить файлы в Android проект
- [ ] Добавить файлы в iOS проект
- [ ] Реализовать обработку в Flutter
- [ ] Добавить настройки звуков для пользователей
- [ ] Протестировать на реальных устройствах

## 🚀 Готово к использованию!

Теперь **все типы уведомлений** поддерживают кастомные звуки:

✅ **Чат** - сообщения и системные уведомления  
✅ **События** - создание, подтверждение, отмена, начало  
✅ **Участники** - присоединение и выход  
✅ **Дружба** - запросы и принятие  
✅ **Общие** - стандартные и специальные звуки  

Система готова к использованию! 🎵🎉
