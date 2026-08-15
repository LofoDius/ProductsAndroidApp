# Контракт REST API

Base URL клиента по умолчанию: `http://10.0.2.2:8080` (эмулятор → host).  
Формат: JSON; изображения — multipart upload / Base64 в ответах.  
Идентификаторы: Mongo `ObjectId` (строка), кроме клиентского синтетического корня `"-1"`.

## Авторизация

| Шаг | Детали |
|-----|--------|
| Регистрация | `POST /auth/register` + `{ "username", "password" }` → `201` + `UserResponse` (без сессии) |
| Логин | `POST /auth/login` + те же поля → `201` + `UserResponse` + header `Authorization: <sessionId>` |
| Дальше | Клиент шлёт `Authorization: Bearer <sessionId>` (на сервере `Bearer ` опционален) |
| Текущий пользователь | `GET /auth/me` |
| Logout | `DELETE /auth/logout` |
| Публичные | только `/auth/register`, `/auth/login` (+ OPTIONS) |
| Каталог | **все** category/card/image/members/search routes требуют сессию |

Shared password и `PUT /password` **удалены**.

## Эндпоинты

### Auth

| Method | Path | Auth | Request | Success |
|--------|------|------|---------|---------|
| POST | `/auth/register` | public | `AuthRequest` | 201 `UserResponse` |
| POST | `/auth/login` | public | `AuthRequest` | 201 `UserResponse` + header `Authorization` |
| DELETE | `/auth/logout` | session | — | 200 |
| GET | `/auth/me` | session | — | 200 `UserResponse` |

### Categories

| Method | Path | Auth | Request | Success |
|--------|------|------|---------|---------|
| GET | `/category/tree` | session + ACL | — | `List<CategoryResponse>` (доступные корни) |
| POST | `/category` | session; child → owner | `CreateCategoryRequest` | `CategoryResponse` |
| PUT | `/category/{id}` | session + owner | `UpdateCategoryRequest` | `CategoryResponse` |
| DELETE | `/category/{id}` | session + owner | — | 200 empty |
| POST | `/category/image` | session | Multipart part `image` | `ImageIdResponse` |
| GET | `/category/image/{id}` | session | — | `ImageResponse` |

### Cards (внутри категории)

| Method | Path | Auth | Request | Success |
|--------|------|------|---------|---------|
| GET | `/category/{categoryId}/cards` | session + access | — | `List<CardResponse>` |
| POST | `/category/{categoryId}/card` | session + access | `CreateCardRequest` | `List<CardResponse>` (все карточки) |
| GET | `/category/{categoryId}/card/{cardId}` | session + access | — | `CardResponse` |
| PUT | `/category/{categoryId}/card/{cardId}` | session + access | `UpdateCardRequest` | `List<CardResponse>` |
| DELETE | `/category/{categoryId}/card/{cardId}` | session + access | — | 200 empty |

### Card images & search

| Method | Path | Auth | Request | Success |
|--------|------|------|---------|---------|
| POST | `/card/image` | session | Multipart part `image` | `ImageIdResponse` |
| GET | `/card/image/{id}` | session | — | `ImageResponse` |
| GET | `/cards/search/{query}` | session + ACL filter | path `query` | `List<CardResponse>` |

### Members

| Method | Path | Auth | Request | Success |
|--------|------|------|---------|---------|
| GET | `/category/{id}/members` | session + access | — | `List<MemberResponse>` |
| POST | `/category/{id}/members` | session + owner | `InviteMemberRequest` | 201 `MemberResponse` |
| DELETE | `/category/{id}/members/{userId}` | session + owner | — | 200 empty |

ACL: invite/remove правят `memberIds` **корня** дерева; list не включает владельца. Роли: см. [02-products-api.md](./02-products-api.md).

## Request DTOs

```kotlin
AuthRequest(username: String, password: String)

CreateCategoryRequest(parentId: ObjectId?, name: String, imageId: String?)
UpdateCategoryRequest(parentId: ObjectId?, name: String, imageId: String?)
// null parentId / imageId при update = оставить текущее значение

CreateCardRequest(
  name: String,
  imageId: String?,
  priceLevel: PriceLevel,
  qualityLevel: QualityLevel,
  rating: Int = 0,          // 0..10; вне диапазона → 400
  description: String?
)
UpdateCardRequest( /* те же поля */ )

InviteMemberRequest(username: String)
```

На Android request-классы зеркалят контракт (`AuthCredentialsRequest`, и т.д.).

## Response DTOs

```kotlin
UserResponse(userId: String, username: String)

CategoryResponse(
  name: String,
  categoryId: String,
  parentId: String?,
  subcategoriesAmount: Int,
  cardsAmount: Int,
  subcategories: List<CategoryResponse>,
  imageId: String?,
  role: CategoryRole  // OWNER | MEMBER
)

CardResponse(
  categoryId: String,
  cardId: String,
  name: String,
  imageId: String?,
  priceLevel: PriceLevel,
  qualityLevel: QualityLevel,
  rating: Int,              // 0..10; у старых документов без поля → 0
  description: String?
)

MemberResponse(userId: String, username: String)
ImageIdResponse(imageId: String)
ImageResponse(image: String)  // Base64
```

## Enums

```kotlin
enum class PriceLevel { LOW_PRICE, MEDIUM_PRICE, HIGH_PRICE }
enum class QualityLevel { LOW_QUALITY, MEDIUM_QUALITY, HIGH_QUALITY }
enum class CategoryRole { OWNER, MEMBER }
```

Имена совпадают на сервере и в Android-клиенте.

## Соответствие клиент ↔ сервер

| Возможность API | Использование в Android UI |
|-----------------|----------------------------|
| POST /auth/register | Да (RegisterScreen + auto-login) |
| POST /auth/login | Да |
| DELETE /auth/logout | Да |
| GET /auth/me | Да (restore session) |
| GET /category/tree | Да |
| CRUD category | Да (owner-gated) |
| category/image upload+get | Да |
| cards list/create/update/delete | Да |
| GET single card | API есть; UI не вызывает |
| card/image upload+get | Да |
| search | Да |
| members list/invite/remove | Да (MembersDialog, owner) |

## Ошибки

Единый JSON:

```json
{ "code": "UNAUTHORIZED", "message": "…" }
```

Типичные `code`: `BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`.  
Сообщения часто на русском. Filter и `GlobalExceptionHandler` используют один shape.
