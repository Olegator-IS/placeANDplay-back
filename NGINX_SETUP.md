# Настройка Nginx для раздачи файлов с SFTP сервера

## Обзор

После загрузки файлов на SFTP сервер, нужно настроить nginx для прямой раздачи файлов через веб-сервер.

## Текущая ситуация

- **Файлы загружаются на**: `/var/www/placeandplay.uz/public_html/uploads/`
- **URL в ответе**: `95.46.96.94/profile-pictures/filename.png`
- **Проблема**: nginx не настроен для раздачи файлов из этой папки

## Шаги настройки

### 1. Подключитесь к серверу

```bash
ssh root@95.46.96.94
```

### 2. Проверьте текущую конфигурацию nginx

```bash
nginx -t
cat /etc/nginx/sites-available/default
```

### 3. Создайте новую конфигурацию

```bash
nano /etc/nginx/sites-available/placeandplay
```

Добавьте следующую конфигурацию:

```nginx
server {
    listen 80;
    server_name 95.46.96.94;  # или ваш домен placeandplay.uz
    
    # Основное приложение Spring Boot
    location /PlaceAndPlay {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Прямая раздача файлов из папки uploads
    location /profile-pictures/ {
        alias /var/www/placeandplay.uz/public_html/uploads/profile-pictures/;
        expires 1y;
        add_header Cache-Control "public, immutable";
        add_header Access-Control-Allow-Origin "*";
        
        # Безопасность - запрещаем выполнение скриптов
        location ~* \.(php|pl|py|jsp|asp|sh|cgi)$ {
            deny all;
        }
    }
    
    location /documents/ {
        alias /var/www/placeandplay.uz/public_html/uploads/documents/;
        expires 1y;
        add_header Cache-Control "public, immutable";
        add_header Access-Control-Allow-Origin "*";
        
        location ~* \.(php|pl|py|jsp|asp|sh|cgi)$ {
            deny all;
        }
    }
    
    location /images/ {
        alias /var/www/placeandplay.uz/public_html/uploads/images/;
        expires 1y;
        add_header Cache-Control "public, immutable";
        add_header Access-Control-Allow-Origin "*";
        
        location ~* \.(php|pl|py|jsp|asp|sh|cgi)$ {
            deny all;
        }
    }
    
    # Общий location для всех файлов в uploads
    location /uploads/ {
        alias /var/www/placeandplay.uz/public_html/uploads/;
        expires 1y;
        add_header Cache-Control "public, immutable";
        add_header Access-Control-Allow-Origin "*";
        
        # Безопасность
        location ~* \.(php|pl|py|jsp|asp|sh|cgi)$ {
            deny all;
        }
    }
    
    # Логирование
    access_log /var/log/nginx/placeandplay_access.log;
    error_log /var/log/nginx/placeandplay_error.log;
}
```

### 4. Активируйте конфигурацию

```bash
# Создайте символическую ссылку
ln -s /etc/nginx/sites-available/placeandplay /etc/nginx/sites-enabled/

# Отключите дефолтную конфигурацию (если нужно)
rm /etc/nginx/sites-enabled/default
```

### 5. Проверьте права доступа

```bash
# Проверьте, что папка существует
ls -la /var/www/placeandplay.uz/public_html/uploads/

# Установите правильные права
chown -R www-data:www-data /var/www/placeandplay.uz/public_html/uploads/
chmod -R 755 /var/www/placeandplay.uz/public_html/uploads/
```

### 6. Проверьте конфигурацию nginx

```bash
nginx -t
```

### 7. Перезапустите nginx

```bash
systemctl reload nginx
# или
systemctl restart nginx
```

## Тестирование

### 1. Проверьте доступность файла

```bash
curl -I http://95.46.96.94/profile-pictures/a2cbd6ba-28ec-4971-b74f-9abda776dbf9_20250815_1310_Secure%20Authentication%20Illustration_remix_01k2pcc33fejfsre4bp8snaxbs%20(2).png
```

### 2. Откройте в браузере

```
http://95.46.96.94/profile-pictures/a2cbd6ba-28ec-4971-b74f-9abda776dbf9_20250815_1310_Secure Authentication Illustration_remix_01k2pcc33fejfsre4bp8snaxbs (2).png
```

## Альтернативные варианты

### Вариант 1: Простая конфигурация

Если у вас уже есть конфигурация для основного приложения, просто добавьте:

```nginx
# Добавьте в существующий server блок
location /profile-pictures/ {
    alias /var/www/placeandplay.uz/public_html/uploads/profile-pictures/;
    expires 1y;
}
```

### Вариант 2: Через API (текущий подход)

Если не хотите настраивать nginx, используйте API эндпоинты:

```
http://95.46.96.94/PlaceAndPlay/api/files/view/profile-pictures/filename.png
```

## Устранение неполадок

### Проблема: 404 Not Found

1. Проверьте путь к файлу:
```bash
ls -la /var/www/placeandplay.uz/public_html/uploads/profile-pictures/
```

2. Проверьте права доступа:
```bash
chown -R www-data:www-data /var/www/placeandplay.uz/public_html/uploads/
```

3. Проверьте логи nginx:
```bash
tail -f /var/log/nginx/error.log
```

### Проблема: 403 Forbidden

1. Проверьте права на папку:
```bash
chmod 755 /var/www/placeandplay.uz/public_html/uploads/
chmod 755 /var/www/placeandplay.uz/public_html/uploads/profile-pictures/
```

2. Проверьте владельца:
```bash
chown www-data:www-data /var/www/placeandplay.uz/public_html/uploads/
```

### Проблема: nginx не перезапускается

1. Проверьте синтаксис:
```bash
nginx -t
```

2. Проверьте конфликты портов:
```bash
netstat -tlnp | grep :80
```

## Проверка работоспособности

После настройки nginx ваш файл должен быть доступен по URL:

```
http://95.46.96.94/profile-pictures/a2cbd6ba-28ec-4971-b74f-9abda776dbf9_20250815_1310_Secure Authentication Illustration_remix_01k2pcc33fejfsre4bp8snaxbs (2).png
```

## Безопасность

1. **Запрет выполнения скриптов**: Конфигурация запрещает выполнение PHP, Python и других скриптов
2. **Кэширование**: Файлы кэшируются на 1 год
3. **CORS**: Разрешены запросы с любого домена (можно ограничить при необходимости)

## Мониторинг

```bash
# Просмотр логов доступа
tail -f /var/log/nginx/placeandplay_access.log

# Просмотр логов ошибок
tail -f /var/log/nginx/placeandplay_error.log

# Статистика запросов
grep "profile-pictures" /var/log/nginx/placeandplay_access.log | wc -l
```
