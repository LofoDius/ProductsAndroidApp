# Модель данных

## Концептуальная схема

```
User (username unique, passwordHash BCrypt)
  │
  └── Session (id = auth token, userId, expiresAt TTL)

Category (document)
  ├── parentId → Category? (null = root)
  ├── ownerId → User
  ├── memberIds[] → User   (только на корне; ACL на всё поддерево)
  ├── cards[] → Card (embedded)
  │     ├── cardId
  │     ├── name, description?
  │     ├── priceLevel, qualityLevel
  │     └── imageId → Image?
  └── imageId → Image?

Image (document)
  └── value: Base64 string (after Thumbnailator)
```

На клиенте дополнительно существует **синтетический корень** с `categoryId = "-1"` («Все категории») — он не хранится на сервере.

**Удалено из модели:** `Password` (shared), `LiteCategory` stubs.

## MongoDB

- Database: `productsDB`
- Доступ: Spring Data `MongoRepository`
- Миграций нет (schemaless); `auto-index-creation=true`
- Индексы: unique `User.username`; TTL на `Session.expiresAt` (`expireAfterSeconds = 0`)
- Имена коллекций по умолчанию Spring Data

## Сущности (server)

### User

| Поле | Тип | Описание |
|------|-----|----------|
| userId | ObjectId (`@Id`) | Идентификатор |
| username | String (unique) | Логин |
| passwordHash | String | BCrypt |
| createdAt | Instant | Создание |

### Session

| Поле | Тип | Описание |
|------|-----|----------|
| id | ObjectId | Токен авторизации |
| userId | ObjectId | Владелец сессии |
| expiresAt | Instant | Истечение; Mongo TTL удаляет документ |

TTL приложения: `app.session.ttl-days` (default 30) при создании сессии на login.

### Category

| Поле | Тип | Описание |
|------|-----|----------|
| categoryId | ObjectId (`@Id`) | Идентификатор |
| name | String | Название |
| parentId | ObjectId? | Родитель; null — корень |
| ownerId | ObjectId | Владелец дерева |
| memberIds | MutableList\<ObjectId\> | Участники; **meaningful только на корне** |
| cards | MutableList\<Card\> | Встроенные карточки |
| imageId | ObjectId? | Картинка категории |

### Card (embedded)

| Поле | Тип | Описание |
|------|-----|----------|
| cardId | ObjectId | Идентификатор |
| name | String | Название |
| imageId | ObjectId? | Картинка |
| priceLevel | PriceLevel | Уровень цены |
| qualityLevel | QualityLevel | Уровень качества |
| description | String? | Описание |

### Image

| Поле | Тип | Описание |
|------|-----|----------|
| imageId | ObjectId | Идентификатор |
| value | String | Base64 содержимое |

## Вспомогательные модели (server, не репозитории)

| Класс | Роль |
|-------|------|
| `FullCategory` | Развёрнутое дерево в памяти (+ `role`) для маппинга в `CategoryResponse` |
| `CategoryRole` | `OWNER` \| `MEMBER` |
| `UserPrincipal` | Principal в SecurityContext после filter |
| `LoginResult` | user + sessionToken внутри AuthService |

## Enums

```kotlin
enum class PriceLevel { LOW_PRICE, MEDIUM_PRICE, HIGH_PRICE }
enum class QualityLevel { LOW_QUALITY, MEDIUM_QUALITY, HIGH_QUALITY }
enum class CategoryRole { OWNER, MEMBER }
```

## Репозитории

```kotlin
UserRepository : MongoRepository<User, String>
  // username unique index; find by username для login/invite

SessionRepository : MongoRepository<Session, String>
  // TTL index на expiresAt

CategoryRepository : MongoRepository<Category, String>
  findByParentId / findByParentIdIsNull
  // + lookup по categoryId

ImageRepository : MongoRepository<Image, String>
  getImageByImageId / deleteImageByImageId
```

## Клиентские модели

Клиент не дублирует domain-слой Mongo: в UI используются response DTO API (+ `UserSession` в domain).

| Клиентский тип | Соответствие |
|----------------|--------------|
| `CategoryResponse` (+ `role`) | Дерево/узел категории |
| `CardResponse` | Карточка |
| `MemberResponse` / `UserSummaryResponse` | Участник / текущий user |
| `ImageResponse` / `ImageIdResponse` | Картинки |
| `PriceLevel` / `QualityLevel` | Те же enum-имена + русские label в UI |
| `CategoryRole` | OWNER / MEMBER |
| `SessionDataStore` + `SessionTokenHolder` | Session id |

## Инварианты и следствия

1. **Карточки не отдельная коллекция** — любая операция с card читает/пишет документ Category.
2. **ACL на корне:** `ownerId` + `memberIds` корня определяют доступ ко всему поддереву; дети не дублируют memberIds.
3. **Дерево** строится через `parentId` lookup (без LiteCategory).
4. **Изображения** отделены; при delete card связанный Image удаляется; при delete category — image категории и рекурсия подкатегорий.
5. **Сессия** привязана к userId и имеет expiresAt (приложение + Mongo TTL).
6. **Пароли** только per-user BCrypt; shared Password-документа нет.
