# Спецификация: Products

Документация системы каталога продуктов: Android-клиент и backend API (multi-user auth + ACL).

| Документ | Содержание |
|----------|------------|
| [00-overview.md](./00-overview.md) | Обзор системы, роли компонентов, ключевые потоки |
| [01-android-app.md](./01-android-app.md) | Реализация Android-приложения (MVVM, Hilt, auth, ACL UI) |
| [02-products-api.md](./02-products-api.md) | Реализация backend API (`products-api`) |
| [03-api-contract.md](./03-api-contract.md) | Контракт REST API (эндпоинты, DTO, auth) |
| [04-data-model.md](./04-data-model.md) | Модель данных и связи |
| [tasks/](./tasks/README.md) | Очередь задач для AI agents (T10–T12) |

> Документы `00`–`04` описывают **текущую** реализацию: per-user auth, session TTL, category ACL owner/member.

**Репозитории:**
- Android: `D:\CursorProjects\ProductsAndroidApp`
- API: `D:\IdeaProjects\products-api` (зеркало API-задач: `products-api/spec/tasks/`)
- iOS: нет
