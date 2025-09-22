# Руководство по доступу к файлам

## Обзор

Файлы загружаются на SFTP сервер и доступны через API эндпоинты. Это обеспечивает безопасность и контроль доступа к файлам.

## Конфигурация

### SFTP сервер
- **Сервер**: 95.46.96.94
- **Порт**: 22
- **Пользователь**: root
- **Папка для файлов**: `/var/www/placeandplay.uz/public_html/uploads`

### Структура папок
```
/var/www/placeandplay.uz/public_html/uploads/
├── profile-pictures/     # Фотографии профилей
├── documents/           # Документы
├── images/             # Общие изображения
└── temp/               # Временные файлы
```

## API эндпоинты для работы с файлами

### 1. Просмотр файла в браузере

**GET** `/api/files/view/{directory}/{filename}`

Позволяет просматривать файлы прямо в браузере (изображения, PDF, и т.д.)

**Пример:**
```
GET /PlaceAndPlay/api/files/view/profile-pictures/6b4dc614-9213-480c-8220-ef923d039688_photo.jpg
```

**Response:**
- **200**: Файл отображается в браузере
- **404**: Файл не найден
- **500**: Ошибка сервера

### 2. Скачивание файла

**GET** `/api/files/download/{directory}/{filename}`

Позволяет скачать файл (с заголовком `Content-Disposition: attachment`)

**Пример:**
```
GET /PlaceAndPlay/api/files/download/profile-pictures/6b4dc614-9213-480c-8220-ef923d039688_photo.jpg
```

**Response:**
- **200**: Файл скачивается
- **404**: Файл не найден
- **500**: Ошибка сервера

## Загрузка файлов

### Профильные фотографии

**POST** `/api/auth/upload-profile-picture`

**Request:**
```multipart
file: [файл изображения]
userId: 123
```

**Response:**
```json
{
    "status": 200,
    "message": "Profile picture uploaded successfully",
    "data": {
        "imageUrl": "https://placeandplay.uz/PlaceAndPlay/api/files/view/profile-pictures/6b4dc614-9213-480c-8220-ef923d039688_photo.jpg"
    }
}
```

## Поддерживаемые форматы файлов

### Изображения
- **JPEG** (.jpg, .jpeg) - `image/jpeg`
- **PNG** (.png) - `image/png`
- **GIF** (.gif) - `image/gif`
- **WebP** (.webp) - `image/webp`

### Документы
- **PDF** (.pdf) - `application/pdf`
- **Текст** (.txt) - `text/plain`
- **HTML** (.html, .htm) - `text/html`

### Архивы
- **ZIP** (.zip) - `application/zip`
- **RAR** (.rar) - `application/x-rar-compressed`

### Медиа
- **MP4** (.mp4) - `video/mp4`
- **MP3** (.mp3) - `audio/mpeg`

## Примеры использования

### 1. Загрузка профильной фотографии

```javascript
const formData = new FormData();
formData.append('file', fileInput.files[0]);
formData.append('userId', '123');

fetch('/PlaceAndPlay/api/auth/upload-profile-picture', {
    method: 'POST',
    body: formData
})
.then(response => response.json())
.then(data => {
    console.log('Image URL:', data.data.imageUrl);
    // Отображаем изображение
    document.getElementById('profile-image').src = data.data.imageUrl;
});
```

### 2. Просмотр изображения

```html
<img src="https://placeandplay.uz/PlaceAndPlay/api/files/view/profile-pictures/filename.jpg" 
     alt="Profile Picture" />
```

### 3. Скачивание файла

```javascript
// Создаем ссылку для скачивания
const downloadUrl = 'https://placeandplay.uz/PlaceAndPlay/api/files/download/profile-pictures/filename.jpg';
const link = document.createElement('a');
link.href = downloadUrl;
link.download = 'filename.jpg';
link.click();
```

## Безопасность

### Ограничения
- **Максимальный размер файла**: 5MB
- **Поддерживаемые форматы**: Только разрешенные типы файлов
- **Доступ**: Файлы доступны только через API

### Рекомендации
1. **Валидация файлов**: Всегда проверяйте тип и размер файла на клиенте
2. **Безопасные URL**: Используйте только API эндпоинты для доступа к файлам
3. **Кэширование**: Используйте кэширование для часто запрашиваемых файлов

## Мониторинг

### Логирование
Все операции с файлами логируются:
```
INFO  - File uploaded successfully to SFTP: /var/www/placeandplay.uz/public_html/uploads/profile-pictures/filename.jpg
INFO  - File downloaded successfully: /var/www/placeandplay.uz/public_html/uploads/profile-pictures/filename.jpg
```

### Ошибки
```
ERROR - Error uploading file: Failed to upload file to SFTP server
ERROR - File not found: /var/www/placeandplay.uz/public_html/uploads/profile-pictures/filename.jpg
```

## Альтернативные решения

### Вариант 1: Настройка nginx для прямой раздачи файлов

Если вы хотите, чтобы файлы были доступны напрямую через веб-сервер, добавьте в конфигурацию nginx:

```nginx
server {
    listen 80;
    server_name placeandplay.uz;
    
    # Основное приложение
    location /PlaceAndPlay {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    # Прямая раздача файлов
    location /uploads/ {
        alias /var/www/placeandplay.uz/public_html/uploads/;
        expires 1y;
        add_header Cache-Control "public, immutable";
        
        # Безопасность
        location ~* \.(php|pl|py|jsp|asp|sh|cgi)$ {
            deny all;
        }
    }
}
```

Тогда URL будут выглядеть так:
```
https://placeandplay.uz/uploads/profile-pictures/filename.jpg
```

### Вариант 2: CDN

Для лучшей производительности можно использовать CDN:
```
https://cdn.placeandplay.uz/uploads/profile-pictures/filename.jpg
```

## Устранение неполадок

### Проблема: Файл не загружается
1. Проверьте размер файла (максимум 5MB)
2. Убедитесь в правильности формата файла
3. Проверьте логи приложения

### Проблема: Файл не отображается
1. Проверьте URL файла
2. Убедитесь, что файл существует на SFTP сервере
3. Проверьте права доступа к файлу

### Проблема: Медленная загрузка
1. Проверьте скорость интернет-соединения
2. Рассмотрите использование CDN
3. Оптимизируйте размер изображений

## Производительность

### Оптимизации
1. **Сжатие изображений**: Используйте WebP формат
2. **Кэширование**: Настройте кэширование в браузере
3. **CDN**: Используйте CDN для статических файлов
4. **Ленивая загрузка**: Загружайте изображения по требованию

### Мониторинг
- Отслеживайте время загрузки файлов
- Мониторьте использование дискового пространства
- Проверяйте количество запросов к файлам
