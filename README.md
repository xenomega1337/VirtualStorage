# Виртуальный склад

Android-приложение для учета вещей и товаров на складе. В приложении можно вести цифровую копию реального склада: добавлять товары с фотографией, распределять их по категориям и подкатегориям, контролировать остатки и видеть, что нужно докупить.

## Как запускать

- Открыть проект в Android Studio.
- Запустить конфигурацию `app` на эмуляторе или Android-устройстве.
- Проверенная сборка: `./gradlew.bat assembleDebug`.

## Основные функции

- Главный экран со статистикой: всего товаров, категорий, единиц и позиций для докупки.
- Добавление, просмотр, редактирование и удаление товаров.
- Добавление фото товара через выбор изображения на устройстве.
- Категории и подкатегории, например `На продажу / Расходники`.
- Поиск товаров по названию и описанию.
- Фильтр товаров по категории.
- Изменение количества кнопками `+1` и `-1`.
- Автоматический список `Докупить`, если остаток меньше или равен минимальному количеству.
- История последних операций.
- Данные сохраняются после перезапуска приложения.
- Склад разделен по аккаунтам: товары одного пользователя не показываются после выхода или входа в другой аккаунт.

## Авторизация и облачная база

В проект добавлены библиотеки Firebase:

- `Firebase Auth` используется для входа и регистрации пользователя на главном экране.
- `Firebase Realtime Database` используется для отправки событий склада в ветку `users/{uid}/inventory_events`.

Важно: для реальной работы Firebase на эмуляторе нужно добавить файл `app/google-services.json` из Firebase Console. Без этого приложение продолжит работать локально через Room, а экран авторизации покажет сообщение о недостающей настройке.

## База данных

Используется локальная Room-база `virtual_storage_room.db`.

Таблицы:

- `categories`: `id`, `user_id`, `name`, `parent_id`.
- `items`: `id`, `user_id`, `name`, `description`, `quantity`, `min_quantity`, `category_id`, `image_uri`, `updated_at`.
- `stock_history`: `id`, `user_id`, `item_id`, `action`, `details`, `created_at`.

Связи:

- `categories.parent_id` ссылается на `categories.id`, поэтому категории могут быть вложенными.
- `items.category_id` ссылается на `categories.id`.
- `stock_history.item_id` ссылается на `items.id`.
- `user_id` хранит Firebase UID пользователя, поэтому локальные данные Room фильтруются по текущему аккаунту.

## Где смотреть код

- UI на Jetpack Compose: `app/src/main/java/com/example/virtualstorage/MainActivity.kt`.
- ViewModel / MVVM: `app/src/main/java/com/example/virtualstorage/InventoryViewModel.kt`.
- Модели данных: `app/src/main/java/com/example/virtualstorage/data/InventoryModels.kt`.
- Room DAO: `app/src/main/java/com/example/virtualstorage/data/InventoryDao.kt`.
- Room база: `app/src/main/java/com/example/virtualstorage/data/InventoryRoomDatabase.kt`.
- CRUD-операции: `app/src/main/java/com/example/virtualstorage/data/InventoryRepository.kt`.
- Firebase Auth / Database: `app/src/main/java/com/example/virtualstorage/data/FirebaseWarehouseRepository.kt`.

## Что показать на демо

1. Запуск приложения.
2. Добавить категорию и подкатегорию.
3. Добавить товар с названием, количеством, минимальным остатком и фото.
4. Найти товар через поиск или фильтр.
5. Изменить количество кнопками `+1` и `-1`.
6. Отредактировать товар.
7. Удалить товар.
8. Показать экран `Докупить`.
9. Перезапустить приложение и показать, что данные сохранились.
