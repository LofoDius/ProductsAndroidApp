# Backend API (products-api)

Репозиторий: `D:\IdeaProjects\products-api`  
Базовый пакет: `lofod.productsapi`

## Стек и сборка

| Параметр | Значение |
|----------|----------|
| Kotlin | 1.9.25 |
| Spring Boot | 3.3.5 |
| JVM | 21 |
| Gradle | 8.10.2 (Kotlin DSL) |
| БД | MongoDB, database `productsDB` |
| Security | Spring Security + кастомный session filter |
| Passwords | BCrypt (`BCryptPasswordEncoder`) |
| Images | Thumbnailator 0.4.20 |
| Packaging | Fat JAR; Docker `amazoncorretto:21-alpine-jdk` |

**Не используется:** JPA, SQL, Flyway/Liquibase, Bean Validation, JWT, OpenAPI/Swagger, shared Password-документ, LiteCategory.

## Конфигурация

`application.properties`:

```properties
spring.application.name=products-api
spring.data.mongodb.uri=${SPRING_MONGO_URI}
spring.data.mongodb.database=productsDB
spring.data.mongodb.auto-index-creation=true
spring.security.filter.order=10
spring.main.allow-circular-references=true
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
app.session.ttl-days=30
```

URI Mongo — из env `SPRING_MONGO_URI`. Auto-index: уникальный `username`, TTL по `Session.expiresAt`.

## Слои

```
Controller → Service → MongoRepository → MongoDB
                ↕
             Mapper (domain → response DTO)
```

```
lofod.productsapi
├── ProductsApiApplication.kt
├── controller/       AuthController, CategoryController
├── service/          Auth, CategoryAccess, Category, Card, Image, Member + mapper/
├── repository/       User, Session, Category, Image
├── model/            entities, enums, request/, response/
├── security/         SecurityConfig, SessionRequestFilter, PasswordEncoderConfig, UserPrincipal
├── exception/        ApiException hierarchy, ErrorResponse, GlobalExceptionHandler
└── util/             ObjectIds
```

## Доменная модель (кратко)

| Сущность | Хранение | Суть |
|----------|----------|------|
| `User` | Mongo document | username (unique), passwordHash (BCrypt), createdAt |
| `Session` | Mongo document | id = токен; userId; expiresAt (TTL index) |
| `Category` | Mongo document | дерево: parentId; ownerId; memberIds на корне; embedded cards; imageId |
| `Card` | Embedded в Category | name, imageId, price/quality, description |
| `Image` | Mongo document | Base64 после сжатия |

Подробнее: [04-data-model.md](./04-data-model.md).

## AuthService

| Операция | Поведение |
|----------|-----------|
| Register | Пустые поля → 400; duplicate username → 409; BCrypt hash; `201` + `UserResponse` (сессии нет) |
| Login | Неверные credentials → 401; создаёт Session с `expiresAt = now + ttlDays`; `201` + body user + header `Authorization: <sessionId hex>` |
| Logout | Удаляет сессию, очищает SecurityContext |
| Me | Текущий `UserPrincipal` → `UserResponse` |
| Validate | Сессия отсутствует/истекла → удаление + 401 |

TTL: `app.session.ttl-days` (default 30). Mongo TTL index `session_expires_at_ttl` на `expiresAt` (`expireAfterSeconds = 0`).

## ACL (CategoryAccessService)

- Роль определяется по **корню** дерева (`ownerId` / `memberIds`); наследуется вниз.
- `requireAccess` — OWNER или MEMBER, иначе 403.
- `requireOwner` — только OWNER, иначе 403.
- Дерево (`GET /category/tree`): только доступные корни; дети через `findByParentId` (без LiteCategory stubs).

| Операция | Роль |
|----------|------|
| Read tree/cards, CRUD cards, search, list members | OWNER или MEMBER |
| Create root category | любой authenticated (становится owner) |
| Create child / update / delete category | OWNER |
| Invite / remove member | OWNER (мутации только на корневом `memberIds`) |
| Image upload/get | authenticated (без category ACL) |

## Бизнес-логика (split services)

| Service | Ответственность |
|---------|-----------------|
| `CategoryService` | дерево, create/update/delete категории (+ subtree), ACL |
| `CardService` | CRUD карточек + search (фильтр по доступу) |
| `ImageService` | upload (Thumbnailator ≤1024 px), get, deleteIfPresent |
| `MemberService` | invite по username / remove / list (без владельца в списке) |
| `CategoryMapper` / `CardMapper` | domain → response DTO |

Update category: `parentId == null` или `imageId == null` в запросе означает **оставить текущее** значение (не сброс).

## Security

### SecurityConfig

- CORS: `*` origins/headers/methods; expose `Authorization`.
- `anyRequest().permitAll()` на уровне Spring Security — реальная проверка в фильтре.
- CSRF off, SESSIONLESS.
- `SessionRequestFilter` перед `UsernamePasswordAuthenticationFilter`.

### SessionRequestFilter — публичные пути

Только exact match:

- `/auth/register`
- `/auth/login`

Также без auth: `OPTIONS`.

Остальное требует заголовок `Authorization` с валидным ObjectId сессии (префикс `Bearer ` опционален). Иначе — `401` + JSON `ErrorResponse`. При успехе в контекст кладётся `UserPrincipal(userId, username)`.

## Ошибки

Единый формат:

```kotlin
data class ErrorResponse(val code: String, val message: String)
```

`GlobalExceptionHandler` мапит `ApiException` (`NOT_FOUND` 404, `FORBIDDEN` 403, `CONFLICT` 409, `UNAUTHORIZED` 401, `BAD_REQUEST` 400) и невалидные ObjectId / тело → 400.  
Ошибки фильтра пишутся в response напрямую тем же JSON-shape.

## Известные особенности

1. Карточки живут внутри документа категории — обновления переписывают родителя.
2. Дерево строится рекурсивно по `parentId` (N+1 lookup детей).
3. `memberIds` meaningful только на корне; invite/remove всегда правят корень.
4. Create card / update card возвращают **весь** список карточек категории.
