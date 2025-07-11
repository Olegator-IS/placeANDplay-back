-- Добавление поля language в таблицу user_details
ALTER TABLE users.user_details 
ADD COLUMN language VARCHAR(5) DEFAULT 'ru';

-- Обновление существующих записей
UPDATE users.user_details 
SET language = 'ru' 
WHERE language IS NULL; 