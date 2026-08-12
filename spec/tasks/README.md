# Задачи на исполнение (AI agents)

Очередь пуста: выполненные задачи удалены перед коммитом.

Новые задачи кладите сюда:

```
spec/tasks/
  api/       — products-api
  android/   — ProductsAndroidApp
  shared/    — спека / кросс-репо
```

**Репозитории:**
- Android (канон): `D:\CursorProjects\ProductsAndroidApp`
- API (зеркало API-задач): `D:\IdeaProjects\products-api\spec\tasks\`

## Формат задачи

Каждый файл содержит frontmatter (`id`, `status`, `depends_on`) и:
- цель и продуктовые решения
- зависимости и out of scope
- acceptance criteria
- ключевые файлы

Перед commit: удалить задачи со `status: done`; если остались незакрытые — не коммитить, спросить пользователя.

## Связанные документы

Актуальная спека: [../README.md](../README.md).
