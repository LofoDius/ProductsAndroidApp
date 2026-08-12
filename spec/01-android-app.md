# Android-приложение (Products)

Пакет / applicationId: `lofod.products`  
Модуль: единственный `:app`  
Язык UI: русский  
Application: `ProductsApp` (`@HiltAndroidApp`)

## Стек и сборка

| Параметр | Значение |
|----------|----------|
| Kotlin | 2.0.0 |
| KSP | 2.0.0-1.0.24 |
| AGP | 8.7.2 |
| Gradle | 8.9 |
| Compose BOM | 2024.11.00 |
| Hilt | 2.52 (+ Navigation Compose 1.2.0) |
| Navigation Compose | 2.8.4 |
| DataStore Preferences | 1.1.1 |
| Lifecycle / ViewModel | 2.8.7 |
| minSdk / targetSdk / compileSdk | 31 / 34 / 35 |
| JVM target | 11 |
| versionName | 1.0.1 |
| UI | Jetpack Compose + Material 3 |
| Сеть | Retrofit 2.9 + OkHttp + Gson + Scalars |
| Картинки | Coil 3 (локальные URI); удалённые — Base64 → Bitmap |

### Конфигурация API

`BuildConfig.API_URL` из `local.properties` (`API_URL`), иначе `http://10.0.2.2:8080`.  
Cleartext разрешён для `10.0.2.2` (`network_security_config.xml`).

## Архитектура

```
ProductsApp (@HiltAndroidApp)
  └─ preload token → SessionTokenHolder
MainActivity (@AndroidEntryPoint)
  └─ AppNavGraph (NavHost)
         ├─ SessionViewModel (bootstrap, logout, 401 → login)
         ├─ Login / Register → AuthViewModel
         └─ Catalog → CatalogViewModel + form/members VMs
```

- **MVVM:** `@HiltViewModel` + `StateFlow` / `SharedFlow`.
- **DI:** Hilt modules `NetworkModule`, `DataStoreModule`.
- **Репозитории:** `AuthRepository`, `CategoryRepository`.
- **Сеть:** Retrofit/OkHttp через Hilt (без object-синглтона).
- **Персистентность сессии:** DataStore (`session_prefs` / ключ `session_token`) + in-memory `SessionTokenHolder`.

## Структура пакетов

```
lofod.products
├── ProductsApp.kt
├── MainActivity.kt
├── di/                 NetworkModule, DataStoreModule
├── domain/             UserSession
├── data/
│   ├── local/          SessionDataStore, SessionTokenHolder
│   ├── remote/         AuthApi, CategoryApi, AuthInterceptor, SessionExpiredNotifier
│   │   ├── model/      PriceLevel, QualityLevel, CategoryRole
│   │   ├── request/    AuthCredentialsRequest, CreateCategory/Card, InviteMember
│   │   └── response/   Category, Card, Member, UserSummary, Image*
│   └── repository/     AuthRepository, CategoryRepository
└── ui/
    ├── navigation/     Routes, AppNavGraph
    ├── session/        SessionViewModel
    ├── auth/           LoginScreen, RegisterScreen, AuthViewModel
    ├── catalog/        CatalogScreen, CatalogViewModel, CatalogAcl, drawer, CardListItem
    ├── category/       CategoryFormDialog, CategoryFormViewModel
    ├── card/           CardFormDialog, CardFormViewModel
    ├── members/        MembersDialog, MembersViewModel
    ├── common/         ErrorMapper, helpers
    └── theme/
```

## Навигация

Маршруты (`Routes`): `login`, `register`, `catalog`.  
Формы категорий/карточек и участники — диалоги поверх каталога, не отдельные destinations.

## UI и ACL

| Экран / composable | Роль |
|--------------------|------|
| `LoginScreen` / `RegisterScreen` | Вход и регистрация (валидация: username не пустой; password ≥ 6) |
| `CatalogScreen` | Drawer, scaffold, поиск, FAB, CRUD-диалоги |
| `CategoryFormDialog` / `CardFormDialog` | Создание/редактирование |
| `MembersDialog` | Список / invite / remove участников (только OWNER) |

Клиентские проверки (`CatalogAcl`):

| Capability | Правило |
|------------|---------|
| Manage category (edit/delete) | не синтетический корень и `OWNER` |
| Manage members | не корень и `OWNER` |
| Create subcategory | синтетический корень **или** `OWNER` |
| Edit cards | любая реальная категория (`OWNER` или `MEMBER`) |

Синтетический корень на клиенте: id `"-1"`, имя «Все категории» (на сервере не хранится).  
В drawer роли показываются как «Владелец» / «Участник».

## Сетевой слой

### NetworkModule

- Base URL: `BuildConfig.API_URL`
- Converters: Scalars, затем Gson (`yyyy-MM-dd'T'HH:mm:ss.SSS`)
- OkHttp: `AuthInterceptor`, timeout 30s, retry on failure; BODY logging только в `DEBUG`

### AuthApi

| Метод | HTTP | Path |
|-------|------|------|
| `register` | POST | `auth/register` |
| `login` | POST | `auth/login` |
| `logout` | DELETE | `auth/logout` |
| `me` | GET | `auth/me` |

### CategoryApi

| Метод | HTTP | Path | Примечание |
|-------|------|------|------------|
| `getCategories` | GET | `category/tree` | |
| `createCategory` | POST | `category` | |
| `updateCategory` | PUT | `category/{id}` | |
| `deleteCategory` | DELETE | `category/{id}` | |
| `uploadCategoryImage` | POST multipart | `category/image` | part `image` |
| `getCategoryImage` | GET | `category/image/{id}` | |
| `getCategoryCards` | GET | `category/{id}/cards` | |
| `createCard` | POST | `category/{id}/card` | |
| `updateCard` | PUT | `category/{id}/card/{cardId}` | |
| `getCard` | GET | `category/{id}/card/{cardId}` | объявлен; UI не использует |
| `deleteCard` | DELETE | `category/{id}/card/{cardId}` | |
| `uploadCardImage` | POST multipart | `card/image` | используется формой карточки |
| `getCardImage` | GET | `card/image/{id}` | |
| `search` | GET | `cards/search/{query}` | |
| `listMembers` | GET | `category/{id}/members` | |
| `inviteMember` | POST | `category/{id}/members` | |
| `removeMember` | DELETE | `category/{id}/members/{userId}` | |

### DTO / enums (клиент)

- `CategoryResponse`: name, categoryId, parentId, counts, nested subcategories, imageId, **role**
- `CardResponse`: cardId, categoryId, name, imageId, priceLevel, qualityLevel, description
- `MemberResponse` / `UserSummaryResponse`: userId, username
- `ImageResponse` / `ImageIdResponse`
- `AuthCredentialsRequest(username, password)`
- `InviteMemberRequest(username)`

```kotlin
enum class PriceLevel { LOW_PRICE, MEDIUM_PRICE, HIGH_PRICE }
// UI labels: «Дешево» / «Средненько» / «Дорого»

enum class QualityLevel { LOW_QUALITY, MEDIUM_QUALITY, HIGH_QUALITY }
// UI labels: «Бич» / «Ну норм» / «Лухари»

enum class CategoryRole { OWNER, MEMBER }
```

## Auth на клиенте

1. **Login:** `POST auth/login` → заголовок `Authorization` (session id) → DataStore + `SessionTokenHolder`.
2. **Register:** `POST auth/register`, затем авто-login (регистрация сама сессию не выдаёт).
3. **Interceptor:** добавляет `Authorization: Bearer <token>`; на HTTP 401 (кроме login/register) очищает токен и шлёт событие в `SessionExpiredNotifier`.
4. **Boot:** `ProductsApp` подгружает токен в holder; `SessionViewModel.restoreSessionIfValid()` вызывает `me()`; при успехе — catalog, иначе — login.
5. **Logout:** `DELETE auth/logout` + очистка локальной сессии.

## Основные сценарии UI

### Boot

1. Spinner / loading через `SessionViewModel`.
2. Authenticated → `CatalogScreen` (загрузка дерева); Unauthenticated → `LoginScreen`.
3. Ошибка загрузки каталога: Retry + Logout.

### Категории и карточки

- Выбор категории → refresh дерева + cards.
- Создание/редактирование через диалоги (родитель — только OWNER-деревья; корневой parent = `null`).
- Карточки: FAB при `canEditCards()`; defaults при create — `LOW_PRICE` / `LOW_QUALITY`.
- Поиск через top bar → тот же список карточек.

### Участники

OWNER открывает «Участники» → list / invite по username / remove.

## Permissions

- `INTERNET`
- Чтение медиа: `READ_EXTERNAL_STORAGE` (≤32), `READ_MEDIA_IMAGES` / `READ_MEDIA_VISUAL_USER_SELECTED` (≥33)
