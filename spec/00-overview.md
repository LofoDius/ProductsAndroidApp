# Обзор системы

## Назначение

Система каталога продуктов: иерархическое дерево категорий и карточки товаров (название, описание, уровни цены/качества, изображения). Пользователи регистрируются и входят по своим учётным данным; доступ к категориям разграничен ACL (владелец / участник).

Корзины и оформления заказа нет.

## Компоненты

```
┌─────────────────────────────┐         REST + Bearer session         ┌─────────────────────────────┐
│  Products Android App       │ ─────────────────────────────────────► │  products-api               │
│  Jetpack Compose + MVVM     │ ◄───────────────────────────────────── │  Spring Boot 3.3 / Kotlin   │
│  Hilt, Retrofit, DataStore  │         JSON / multipart / Base64      │  MongoDB (productsDB)       │
└─────────────────────────────┘                                        └─────────────────────────────┘
```

| Компонент | Стек | Роль |
|-----------|------|------|
| Android app | Kotlin 2.0, Compose, Hilt, Navigation, DataStore, Retrofit | UI, сессия пользователя, ACL-aware каталог, in-app обновление APK |
| products-api | Spring Boot 3.3.5, Kotlin 1.9, MongoDB | Auth, ACL, каталог, изображения, хостинг APK-релизов |

## Архитектурный стиль

- **Клиент:** MVVM (ViewModel + StateFlow) → Repository → Retrofit; DI через Hilt; токен в DataStore.
- **Сервер:** Controller → Service → MongoRepository; карточки встроены в документ категории; ACL на корне дерева.
- **Связь:** HTTP REST; почти все эндпоинты требуют session-токен в `Authorization`. Публичны: register/login и проверка/скачивание/публикация APK (`/app/*`; публикация — по `X-Deploy-Token`).

## Ключевые пользовательские потоки

1. **Регистрация / вход:** `POST /auth/register` или `POST /auth/login`; при логине клиент сохраняет session id из заголовка `Authorization`.
2. **Старт с сохранённой сессией:** DataStore → проверка `GET /auth/me` → каталог или экран логина.
3. **Каталог:** drawer с деревьями, к которым у пользователя есть доступ (owner или member); выбор категории → карточки.
4. **CRUD:** владелец управляет категориями и участниками; владелец и участник — карточками в доступном дереве.
5. **Приглашение:** владелец добавляет участника по username (`POST /category/{id}/members`).
6. **Изображения / поиск:** multipart upload → `imageId`; получение Base64; поиск карточек в доступных категориях.
7. **Обновление приложения:** при старте клиент спрашивает `GET /app/latest` (без сессии); если `versionCode` новее установленного — диалог, скачивание APK и установка. CI Android-репозитория собирает signed APK и публикует его на API (`POST /app/releases`).

## Auth и ACL (кратко)

- **Per-user:** username + BCrypt-пароль; сессия в Mongo с TTL (~30 дней).
- Клиент шлёт `Authorization: Bearer <sessionId>`; токен персистится в DataStore.
- **ACL:** у корневой категории `ownerId` + `memberIds`; права наследуются всем поддеревом. Роли: `OWNER` | `MEMBER`.
- Shared password / публичное чтение каталога **нет**.

Подробности: [03-api-contract.md](./03-api-contract.md), [01-android-app.md](./01-android-app.md), [04-data-model.md](./04-data-model.md).
