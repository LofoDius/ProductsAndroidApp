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
  ├── customFields[] → CustomFieldDefinition   (активные, ≤10)
  ├── customFieldArchive[] → CustomFieldDefinition
  ├── cards[] → Card (embedded)
  │     ├── cardId
  │     ├── name, description?
  │     ├── priceLevel, qualityLevel
  │     ├── rating (0..10; отсутствует в старых docs → 0)
  │     ├── customFieldValues[] → { fieldId, value? }
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
| customFields | List\<CustomFieldDefinition\> | Активная схема (≤10); старые docs → empty |
| customFieldArchive | List\<CustomFieldDefinition\> | Снятые поля для restore; значения на cards не purge |

### Card (embedded)

| Поле | Тип | Описание |
|------|-----|----------|
| cardId | ObjectId | Идентификатор |
| name | String | Название |
| imageId | ObjectId? | Картинка |
| priceLevel | PriceLevel | Уровень цены |
| qualityLevel | QualityLevel | Уровень качества |
| rating | Int | Рейтинг 0..10 (шаг 1); default `0` если поля нет в Mongo |
| description | String? | Описание |
| customFieldValues | List\<CustomFieldValue\> | Значения по fieldId; orphans архивных полей сохраняются |

### CustomFieldDefinition (embedded)

| Поле | Тип | Описание |
|------|-----|----------|
| fieldId | ObjectId | Стабильный id (для restore и значений на cards) |
| title | String | Отображаемое название |
| type | CustomFieldType | TEXT / NUMBER / BOOLEAN / DATE / COUNTER |

### CustomFieldValue (embedded in Card)

| Поле | Тип | Описание |
|------|-----|----------|
| fieldId | ObjectId | Ссылка на definition (active или archive) |
| value | String? | Строковое значение; формат зависит от type |

### Image

| Поле | Тип | Описание |
|------|-----|----------|
| imageId | ObjectId | Идентификатор |
| value | String | Base64 содержимое |

## Вспомогательные модели (server, не репозитории)

| Класс | Роль |
|-------|------|
| `FullCategory` | Развёрнутое дерево в памяти (+ `role`, custom fields) для маппинга в `CategoryResponse` |
| `CategoryRole` | `OWNER` \| `MEMBER` |
| `UserPrincipal` | Principal в SecurityContext после filter |
| `LoginResult` | user + sessionToken внутри AuthService |

## Enums

```kotlin
enum class PriceLevel { LOW_PRICE, MEDIUM_PRICE, HIGH_PRICE }
enum class QualityLevel { LOW_QUALITY, MEDIUM_QUALITY, HIGH_QUALITY }
enum class CategoryRole { OWNER, MEMBER }
enum class CustomFieldType { TEXT, NUMBER, BOOLEAN, DATE, COUNTER }
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
| `CategoryResponse` (+ `role`, customFields, archive) | Дерево/узел категории |
| `CardResponse` (+ customFieldValues) | Карточка |
| `CustomFieldDefinitionDto` / `CustomFieldValueDto` | Схема / значения |
| `MemberResponse` / `UserSummaryResponse` | Участник / текущий user |
| `ImageResponse` / `ImageIdResponse` | Картинки |
| `PriceLevel` / `QualityLevel` / `CustomFieldType` | Те же enum-имена (+ русские label в UI где нужно) |
| `CategoryRole` | OWNER / MEMBER |
| `SessionDataStore` + `SessionTokenHolder` | Session id |

## Инварианты и следствия

1. **Карточки не отдельная коллекция** — любая операция с card читает/пишет документ Category.
2. **ACL на корне:** `ownerId` + `memberIds` корня определяют доступ ко всему поддереву; дети не дублируют memberIds.
3. **Дерево** строится через `parentId` lookup (без LiteCategory).
4. **Изображения** отделены; при delete card связанный Image удаляется; при delete category — image категории и рекурсия подкатегорий.
5. **Сессия** привязана к userId и имеет expiresAt (приложение + Mongo TTL).
6. **Пароли** только per-user BCrypt; shared Password-документа нет.
7. **Custom fields:** активных ≤10; удаление из схемы → archive, не purge значений на cards; restore по fieldId или title+type; при save card merge orphans.
