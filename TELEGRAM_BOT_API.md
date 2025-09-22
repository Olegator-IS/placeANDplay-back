# Telegram Bot API Documentation

## Обзор

Этот API предназначен для интеграции с Telegram ботом для организаций. Все эндпоинты находятся под базовым URL `/api/bot/organizations` и не требуют проверки безопасности через headers.

## Эндпоинты

### 1. Авторизация организации

**POST** `/api/bot/organizations/login`

Авторизация организации через email и password.

**Request Body:**
```json
{
    "email": "organization@example.com",
    "password": "password123"
}
```

**Response (200):**
```json
{
    "status": 200,
    "accessToken": "encrypted_access_token",
    "refreshToken": "encrypted_refresh_token"
}
```

**Response (401):**
```json
{
    "status": 401,
    "message": "Invalid email or password",
    "error": "INVALID_CREDENTIALS"
}
```

### 2. Получение информации об организации

**POST** `/api/bot/organizations/orgInfo`

Получение информации об организации по токену.

**Request Body:**
```json
{
    "accessToken": "encrypted_access_token",
    "refreshToken": "encrypted_refresh_token"
}
```

**Response (200):**
```json
{
    "status": 200,
    "data": {
        "orgId": 123,
        "email": "organization@example.com",
        "phone": "+1234567890",
        "status": "ACTIVE",
        "organization": {
            "orgId": 123,
            "orgType": "SPORTS_CLUB",
            "status": "ACTIVE",
            "rating": 4.5,
            "address": "123 Main St"
        }
    }
}
```

### 3. Сохранение Telegram chat ID

**POST** `/api/bot/organizations/telegram-chat`

Сохранение связи организации с Telegram chat ID.

**Request Body:**
```json
{
    "orgId": 123,
    "chatId": "-1001234567890",
    "chatName": "Main Organization Chat",
    "botToken": "optional_bot_token"
}
```

**Response (200):**
```json
{
    "id": 1,
    "orgId": 123,
    "chatId": "-1001234567890",
    "chatName": "Main Organization Chat",
    "botToken": "optional_bot_token",
    "isActive": true,
    "createdAt": "2024-01-01T12:00:00",
    "updatedAt": "2024-01-01T12:00:00"
}
```

### 4. Получение всех chat ID организации

**GET** `/api/bot/organizations/telegram-chats/{orgId}`

Получение всех активных Telegram chat ID для организации.

**Response (200):**
```json
[
    {
        "id": 1,
        "orgId": 123,
        "chatId": "-1001234567890",
        "chatName": "Main Chat",
        "isActive": true
    },
    {
        "id": 2,
        "orgId": 123,
        "chatId": "-1009876543210",
        "chatName": "Support Chat",
        "isActive": true
    }
]
```

### 5. Деактивация chat ID

**DELETE** `/api/bot/organizations/telegram-chat/{orgId}/{chatId}`

Деактивация конкретного chat ID для организации.

**Response (200):**
```
"Chat deactivated successfully"
```

## Пример использования в Telegram боте

### 1. Авторизация
```python
import requests

# Авторизация организации
login_data = {
    "email": "organization@example.com",
    "password": "password123"
}

response = requests.post(
    "https://your-domain.com/PlaceAndPlay/api/bot/organizations/login",
    json=login_data
)

if response.status_code == 200:
    tokens = response.json()
    access_token = tokens["accessToken"]
    refresh_token = tokens["refreshToken"]
    print("Авторизация успешна")
else:
    print("Ошибка авторизации")
```

### 2. Получение информации об организации
```python
# Получение информации об организации
org_info_data = {
    "accessToken": access_token,
    "refreshToken": refresh_token
}

response = requests.post(
    "https://your-domain.com/PlaceAndPlay/api/bot/organizations/orgInfo",
    json=org_info_data
)

if response.status_code == 200:
    org_info = response.json()
    org_id = org_info["data"]["orgId"]
    print(f"ID организации: {org_id}")
```

### 3. Сохранение chat ID
```python
# Сохранение chat ID
chat_data = {
    "orgId": org_id,
    "chatId": str(chat_id),  # chat_id из Telegram
    "chatName": "Main Organization Chat"
}

response = requests.post(
    "https://your-domain.com/PlaceAndPlay/api/bot/organizations/telegram-chat",
    json=chat_data
)

if response.status_code == 200:
    print("Chat ID успешно сохранен")
```

## Отправка уведомлений

После сохранения chat ID, система может отправлять уведомления через Telegram API:

**POST** `/api/notifications/telegram/send/organization/{orgId}`

**Request Body:**
```json
{
    "message": "Новое уведомление для организации!"
}
```

## Особенности

1. **Безопасность**: API не проверяет headers, поэтому предназначен только для внутреннего использования ботом
2. **Множественные chat ID**: Одна организация может иметь несколько активных chat ID
3. **Мягкое удаление**: Chat ID деактивируются, а не удаляются физически
4. **Поддержка HTML**: Сообщения поддерживают HTML разметку
5. **Логирование**: Все операции логируются для отладки

## Обработка ошибок

- **400**: Неверный формат запроса
- **401**: Неверные учетные данные или неактивный аккаунт
- **404**: Организация или chat не найден
- **409**: Chat ID уже существует для организации
- **500**: Внутренняя ошибка сервера

## Сценарий использования

1. **Бот авторизует организацию:**
   ```
   POST /api/bot/organizations/login
   {
     "email": "org@example.com",
     "password": "password123"
   }
   ```

2. **Получает информацию об организации:**
   ```
   POST /api/bot/organizations/orgInfo
   {
     "accessToken": "encrypted_token",
     "refreshToken": "encrypted_refresh_token"
   }
   ```

3. **Сохраняет chat ID:**
   ```
   POST /api/bot/organizations/telegram-chat
   {
     "orgId": 123,
     "chatId": "-1001234567890",
     "chatName": "Main Chat"
   }
   ```

4. **Система отправляет уведомления:**
   ```
   POST /api/notifications/telegram/send/organization/123
   {
     "message": "Новое уведомление!"
   }
   ```

## Интеграция с существующей системой

- API использует существующие сервисы организаций
- Поддерживает множественные chat ID для одной организации
- Интегрируется с системой уведомлений
- Использует существующую аутентификацию и авторизацию
