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
| versionName | 1.0.1 (локально); CI задаёт `1.0.${{ github.run_number }}` |
| UI | Jetpack Compose + Material 3 |
| Сеть | Retrofit 2.9 + OkHttp + Gson + Scalars |
| Картинки | Coil 3 (локальные URI); удалённые — Base64 → Bitmap |

### Конфигурация API

`BuildConfig.API_URL` из env `API_BASE_URL`, иначе `local.properties` (`API_URL`), иначе `http://10.0.2.2:8080`.  
Cleartext разрешён для `10.0.2.2` (`network_security_config.xml`).

CI (`.github/workflows/release.yml` на push в `master`) подставляет `API_BASE_URL` из секрета `DEPLOY_API_URL`.

## Архитектура

```
ProductsApp (@HiltAndroidApp)
  └─ preload token → SessionTokenHolder
MainActivity (@AndroidEntryPoint)
  └─ AppNavGraph (NavHost)
         ├─ AppUpdateHost / AppUpdateViewModel (проверка обновления и на login)
         ├─ SessionViewModel (bootstrap, logout, 401 → login)
         ├─ Login / Register → AuthViewModel
         └─ Catalog → CatalogViewModel + form/members VMs
```

- **MVVM:** `@HiltViewModel` + `StateFlow` / `SharedFlow`.
- **DI:** Hilt modules `NetworkModule`, `DataStoreModule`, `AppUpdateModule`.
- **Репозитории:** `AuthRepository`, `CategoryRepository`, `AppUpdateRepository`.
- **Сеть:** Retrofit/OkHttp через Hilt (без object-синглтона); отдельный OkHttp без `AuthInterceptor` для `/app/*`.
- **Персистентность сессии:** DataStore (`session_prefs` / ключ `session_token`) + in-memory `SessionTokenHolder`.

## Структура пакетов

```
lofod.products
├── ProductsApp.kt
├── MainActivity.kt
├── di/                 NetworkModule, DataStoreModule
├── domain/             UserSession
├── data/
│   ├── local/          SessionDataStore, SessionTokenHolder, AppUpdateDataStore
│   ├── remote/         AuthApi, CategoryApi, AppUpdateApi, AuthInterceptor, SessionExpiredNotifier
│   │   ├── model/      PriceLevel, QualityLevel, CategoryRole
│   │   ├── request/    AuthCredentialsRequest, CreateCategory/Card, InviteMember
│   │   └── response/   Category, Card, Member, UserSummary, Image*, AppReleaseDto
│   └── repository/     AuthRepository, CategoryRepository, AppUpdateRepository
└── ui/
    ├── navigation/     Routes, AppNavGraph
    ├── session/        SessionViewModel
    ├── auth/           LoginScreen, RegisterScreen, AuthViewModel
    ├── catalog/        CatalogScreen, CatalogViewModel, CatalogAcl, drawer, CardListItem
    ├── category/       CategoryFormScreen, CategoryFormViewModel
    ├── card/           CardFormScreen, CardFormViewModel
    ├── members/        MembersScreen, MembersViewModel
    ├── update/         AppUpdateHost, AppUpdateViewModel, ApkInstaller
    ├── common/         ErrorMapper, ButtonProgressIndicator, RatingBar, CategoryIcon / RemoteImage
    └── theme/
```

## Навигация

Маршруты (`Routes`):

| Route | Экран |
|-------|--------|
| `login` / `register` | auth |
| `catalog` | `CatalogScreen` |
| `card/create/{categoryId}`, `card/edit/{categoryId}/{cardId}` | `CardFormScreen` |
| `category/create/{parentId}`, `category/edit/{categoryId}` | `CategoryFormScreen` |
| `category/{categoryId}/members` | `MembersScreen` |

После успешного save форма ставит флаг на `SavedStateHandle` каталога: `KEY_CARD_FORM_SAVED` / `KEY_CATEGORY_FORM_SAVED` (каталог читает и сбрасывает).  
Подтверждение удаления категории/карточки — `AlertDialog` на `CatalogScreen` (не отдельный route).

## UI и ACL

Material Design 3: `NavigationDrawerItem` / `ListItem` в drawer, `TopAppBar` + snackbar, `ExposedDropdownMenuBox` в форме оценки, dynamic color (fallback — нейтральная зелёная схема, не шаблонный purple).

| Экран / composable | Роль |
|--------------------|------|
| `LoginScreen` / `RegisterScreen` | Вход и регистрация (валидация: username не пустой; password ≥ 6); `Scaffold` + Snackbar |
| `CatalogScreen` | Drawer, scaffold, поиск, FAB, snackbar; иконка категории в drawer и в title; delete confirms — `AlertDialog` |
| `CategoryFormScreen` | Полноэкранная create/edit категории (OWNER); схема custom fields (≤10) |
| `CardFormScreen` | Полноэкранная create/edit оценки; `RatingBar`; edit через `GET category/{id}/card/{cardId}`; значения custom fields |
| `MembersScreen` | Полноэкранный список / invite / remove участников (только OWNER) |

Клиентские проверки (`CatalogAcl`):

| Capability | Правило |
|------------|---------|
| Manage category (edit/delete) | не синтетический корень и `OWNER` |
| Manage members | не корень и `OWNER` |
| Create subcategory | синтетический корень **или** `OWNER` |
| Edit cards | любая реальная категория (`OWNER` или `MEMBER`) |

Синтетический корень на клиенте: id `"-1"`, имя «Все категории» (на сервере не хранится).  
В drawer роли показываются как «Владелец» / «Участник».  
Иконка категории (`imageId` → `getCategoryImage`, иначе placeholder Folder): в header/строках drawer и в title top bar (не для synthetic root / режима поиска).  
Пустой каталог (синтетический корень без top-level категорий): в drawer одно сообщение «Нет категорий» вместо заголовка «Все категории» / «Выберите категорию» / «Нет подкатегорий»; пустая ветка внутри существующей категории по-прежнему «Нет подкатегорий».  
В списке оценок (`CardListItem`) блок изображения показывается только при успешно загруженном `imageId`; без картинки и при ошибке загрузки placeholder не рисуется.  
Рейтинг карточки: API `rating` Int 0..10; UI — 5 звёзд (половина = +1). В `CardListItem` — read-only `RatingBar`; в `CardFormScreen` — интерактивный (tap / scrub). Default при создании: `0`.

### Пользовательские поля (custom fields)

- **Схема** (активные ≤10 + архив): правит только OWNER на `CategoryFormScreen`; уходит в `CreateCategoryRequest` / update category.
- **Значения** на карточке: заполняют OWNER и MEMBER на `CardFormScreen` по активной схеме категории (`customFieldValues`: `fieldId` + string `value`).
- Типы: `TEXT`, `NUMBER`, `BOOLEAN`, `DATE`, `COUNTER` (см. [03-api-contract.md](./03-api-contract.md)).
- UI схемы на форме категории: подсказки title — dropdown из `customFieldArchive`, отфильтрованный по выбранному типу; по умолчанию title **пустой** (без autofill); выбор подсказки выставляет `title` + `fieldId` (restore).

## Сетевой слой

### NetworkModule

- Base URL: `BuildConfig.API_URL`
- Converters: Scalars, затем Gson (`yyyy-MM-dd'T'HH:mm:ss.SSS`)
- OkHttp (основной): `AuthInterceptor`, timeout 30s, retry on failure; BODY logging только в `DEBUG`
- OkHttp обновлений (`@AppUpdateNetwork`): без auth, read/write 60s, без call timeout; HEADERS logging в `DEBUG`

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
| `getCard` | GET | `category/{id}/card/{cardId}` | используется `CardFormScreen` (edit) |
| `deleteCard` | DELETE | `category/{id}/card/{cardId}` | |
| `uploadCardImage` | POST multipart | `card/image` | используется формой карточки |
| `getCardImage` | GET | `card/image/{id}` | |
| `search` | GET | `cards/search/{query}` | |
| `listMembers` | GET | `category/{id}/members` | |
| `inviteMember` | POST | `category/{id}/members` | |
| `removeMember` | DELETE | `category/{id}/members/{userId}` | |

### AppUpdateApi (без сессии)

| Метод | HTTP | Path |
|-------|------|------|
| `getLatestRelease` | GET | `app/latest` |
| `downloadApk` | GET streaming | `app/download` (или `downloadPath` из latest) |

DTO: `AppReleaseDto(versionCode, versionName, releasedAt, downloadPath)`.

### DTO / enums (клиент)

- `CategoryResponse`: name, categoryId, parentId, counts, nested subcategories, imageId, **role**, **customFields**, **customFieldArchive**
- `CardResponse`: cardId, categoryId, name, imageId, priceLevel, qualityLevel, **rating** (0..10, default 0), description, **customFieldValues**
- `CustomFieldDefinitionDto(fieldId?, title, type)`, `CustomFieldValueDto(fieldId, value?)`
- `MemberResponse` / `UserSummaryResponse`: userId, username
- `ImageResponse` / `ImageIdResponse`
- `AuthCredentialsRequest(username, password)`
- `InviteMemberRequest(username)`
- `AppReleaseDto(versionCode, versionName, releasedAt, downloadPath)`

```kotlin
enum class PriceLevel { LOW_PRICE, MEDIUM_PRICE, HIGH_PRICE }
// UI labels: «Дешево» / «Средненько» / «Дорого»

enum class QualityLevel { LOW_QUALITY, MEDIUM_QUALITY, HIGH_QUALITY }
// UI labels: «Бич» / «Ну норм» / «Лухари»

enum class CategoryRole { OWNER, MEMBER }

enum class CustomFieldType { TEXT, NUMBER, BOOLEAN, DATE, COUNTER }
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

- Выбор категории → refresh дерева + cards; иконка категории в drawer и title.
- Создание/редактирование категории — `CategoryFormScreen` (`category/create/{parentId}`, `category/edit/{categoryId}`); родитель — только OWNER-деревья; корневой parent = `null`; schema custom fields — OWNER.
- Карточки: FAB / edit → `CardFormScreen` (`card/create/...`, `card/edit/...`); defaults при create — `LOW_PRICE` / `LOW_QUALITY` / `rating = 0`; значения custom fields по активной схеме.
- Форма оценки: `ExposedDropdownMenu` для `priceLevel` / `qualityLevel` и интерактивный рейтинг звёздами (`RatingBar`); список показывает рейтинг read-only.
- Поиск через top bar → тот же список карточек.
- Удаление категории/карточки — confirm `AlertDialog` на каталоге.
- Участники — `MembersScreen` (`category/{categoryId}/members`).

### Участники

OWNER открывает «Участники» → list / invite по username / remove.

### Обновление приложения

1. `AppUpdateHost` над NavHost проверяет `GET /app/latest` один раз за процесс (и на экране логина).
2. Если `versionCode` сервера больше `BuildConfig.VERSION_CODE` и пользователь не откладывал эту версию — диалог «Обновить / Позже».
3. «Позже» пишет `versionCode` в DataStore (`app_update`); пункт «Обновить приложение» остаётся в drawer, пока релиз новее установленного.
4. Скачивание стримом в `cacheDir/app_updates/`, затем FileProvider → системный установщик. Нужно разрешение «установка неизвестных приложений».

## CI / релиз

`.github/workflows/release.yml` (push в `master` или `workflow_dispatch`):

1. JDK 21 + Android SDK; decode keystore из `SIGNING_KEYSTORE_BASE64`.
2. `assembleRelease` с `-PversionCode=${{ github.run_number }}` `-PversionName=1.0.${{ github.run_number }}`; `API_BASE_URL` = `DEPLOY_API_URL`.
3. `POST $DEPLOY_API_URL/app/releases` с `X-Deploy-Token` и multipart APK.
4. Artifact `products-release-<run_number>`.

Секреты репозитория Android: `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`, `DEPLOY_API_URL`, `DEPLOY_TOKEN`.

## Permissions

- `INTERNET`
- `REQUEST_INSTALL_PACKAGES` (in-app install APK)
- Чтение медиа: `READ_EXTERNAL_STORAGE` (≤32), `READ_MEDIA_IMAGES` / `READ_MEDIA_VISUAL_USER_SELECTED` (≥33)
