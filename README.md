# 🛡️ PlaceAndPlay

**PlaceAndPlay** — это backend-сервис аутентификации и авторизации для мобильного приложения [Place&Play](https://github.com/Olegator-IS/placeandplay), социального сервиса для поиска спортивных партнёров и мероприятий.

---

## 🚀 Возможности

- Регистрация и вход пользователей  
- JWT-аутентификация (Bearer-токены)  
- Хранение и верификация паролей  
- Работа с профилями и ролями  
- Создание ивентов (спортивных мероприятий)  
- Присоединение к ивентам другими пользователями  
- Встроенный чат внутри ивентов  
- Редактирование профиля пользователя  
- Просмотр событий (лентой или по фильтрам)

---

## 🛠️ Технологии

- Java 23  
- Spring Boot 2.5.5  
- Spring Security  
- PostgreSQL  
- JWT (JSON Web Tokens)  
- Gradle (Groovy)

---

## 📁 Структура проекта

<pre>
src/
├── config/         # Настройки безопасности и CORS
├── controller/     # REST-контроллеры
├── model/          # DTO и сущности
├── repository/     # JPA-репозитории
├── service/        # Бизнес-логика
</pre>

---

## 🧠 Архитектура

Проект построен по принципам многослойной архитектуры:

- **Controller** — принимает HTTP-запросы  
- **Service** — бизнес-логика  
- **Repository** — взаимодействие с БД (JPA)  
- **Security Config** — настройка Spring Security и JWT

---

## ⚙️ Запуск проекта

```bash
./gradlew bootRun
```

Или через IDE, запустив `PlaceAndPlayApplication.java`.

---

## 📬 Контакты

**Разработчик**: Олег [@Olegator-IS](https://github.com/Olegator-IS)  
**Email**: [business@placeandplay.uz](mailto:business@placeandplay.uz)

---
