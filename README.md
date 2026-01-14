# 🧩 ExploreWithMe: Архитектура микросервисов (Этап 2)

На втором этапе монолитное приложение **ExploreWithMe** было декомпозировано на набор независимых микросервисов с целью
повышения масштабируемости, отказоустойчивости и упрощения дальнейшей поддержки.

Вся система теперь состоит из функционально обособленных компонентов, взаимодействующих через REST API и объединённых
инфраструктурными сервисами.

---

## 🏗️ Состав системы

### Основные микросервисы

| Сервис                  | Ответственность                                                             |
|-------------------------|-----------------------------------------------------------------------------|
| **event-service**       | Управление событиями: создание, публикация, редактирование, поиск, просмотр |
| **user-service**        | Работа с пользователями: регистрация, валидация, получение данных           |
| **request-service**     | Обработка заявок на участие в событиях: создание, подтверждение, отклонение |
| **comment-service**     | Управление комментариями к событиям: добавление, модерация, отображение     |
| **compilation-service** | Создание и управление подборками событий                                    |

### Инфраструктурные компоненты

| Компонент            | Назначение                                           |
|----------------------|------------------------------------------------------|
| **gateway-server**   | Единая точка входа для всех клиентских запросов      |
| **discovery-server** | Сервис регистрации и обнаружения (Eureka)            |
| **config-server**    | Централизованное хранение конфигураций всех сервисов |
| **stats-server**     | Сбор и агрегация статистики по просмотрам событий    |

### Общие зависимости

- **`interaction-api`** — библиотека с общими DTO, Feign-клиентами и исключениями.
- **`stats-dto`** — DTO для взаимодействий со stats-сервисом.

---

## 🔌 Внутреннее взаимодействие

Микросервисы общаются между собой посредством **REST-вызовов** с использованием **OpenFeign**. Все клиентские интерфейсы
определены в модуле `interaction-api`.

Примеры ключевых взаимодействий:

- **`event-service` → `user-service`**  
  Получение информации об авторе события.
- **`event-service` → `request-service`**  
  Запрос списка заявок и количества подтверждённых участников.
- **`request-service` → `event-service`**  
  Обновление поля `confirmedRequests` после изменения статуса заявок.
- **`comment-service` → `user-service` / `event-service`**  
  Валидация существования пользователя и события при создании комментария.
- **Все сервисы → `stats-server`**  
  Отправка данных о просмотрах и получение статистики.

Для повышения надёжности реализованы **fallback-методы** в Feign-клиентах. Например, при недоступности `request-service`
поле `confirmedRequests` возвращается как `0`.

---

## ⚙️ Конфигурация и развёртывание

- Каждый микросервис имеет **собственную базу данных PostgreSQL**.
- Все настройки (подключение к БД, Eureka, порты и т.д.) загружаются из **Config Server**.
- Сервисы автоматически регистрируются в **Eureka Discovery Server**.
- Все внешние запросы проходят через **API Gateway** (`http://localhost:8080`), что скрывает внутреннюю архитектуру от
  клиента.

---

## 📡 Внешний API

Публичный и административный API полностью соответствует официальной спецификации проекта:

- [main-service-specification](https://github.com/igrshrn/java-plus-graduation/blob/main/ewm-main-service-spec.json)
- [stats-service-specification](https://github.com/igrshrn/java-plus-graduation/blob/main/ewm-stats-service-spec.json)

---

## ✅ Надёжность

- При недоступности одного из сервисов остальные продолжают работать.
- Fallback-логика предотвращает каскадные сбои.
- Система проходит все Postman-тесты даже при остановке отдельных микросервисов.

> 💡 Проект готов к горизонтальному масштабированию: каждый сервис может быть развёрнут в нескольких экземплярах
> независимо от других.

## 🚀 Как запустить проект локально

> ⚠️ **Важно**: все сервисы зависят от **Discovery Server** и **Config Server**. Запускайте их **в указанном порядке**.

### 1. Запустите Discovery Server (Eureka)

Это центр регистрации и обнаружения сервисов.

```bash
cd infra/discovery-server
mvn spring-boot:run
```

### 2. Запустите Config Server

Сервер централизованной конфигурации.

```bash
cd infra/config-server
mvn spring-boot:run
```

`Доступен по адресу: http://localhost:8888`

Конфигурации хранятся в infra/config-server/src/main/resources/config/

### 3. Запустите основные микросервисы

Запускайте в любом порядке (или параллельно):

```bash
# User Service
cd core/user-service && mvn spring-boot:run

# Category Service
cd core/category-service && mvn spring-boot:run

# Event Service
cd core/event-service && mvn spring-boot:run

# Comment Service
cd core/comment-service && mvn spring-boot:run

# Request Service
cd core/request-service && mvn spring-boot:run

# Compilation Service
cd core/compilation-service && mvn spring-boot:run

# Stats Server (опционально, но рекомендуется)
cd stats/stats-server && mvn spring-boot:run
```

### 4. Запустите Gateway Server

Единая точка входа для всех запросов.

`Доступен по адресу: http://localhost:8080`

## 🗃 Базы данных

Каждый сервис использует отдельную PostgreSQL-базу. Убедитесь, что у вас установлен **PostgreSQL** и созданы следующие
базы данных:

| Сервис                | Имя базы данных       | Порт |
|-----------------------|-----------------------|------|
| `user-service`        | `user_service`        | 5432 |
| `category-service`    | `category_service`    | 5432 |
| `event-service`       | `event_service`       | 5432 |
| `comment-service`     | `comment_service`     | 5432 |
| `request-service`     | `request_service`     | 5432 |
| `compilation-service` | `compilation_service` | 5432 |
| `stats-server`        | `stats_db`            | 5432 |

> 💡 По умолчанию в конфигурационных файлах используются следующие параметры подключения:
> - **host**: `localhost`
> - **port**: `5432`
> - **username / password**: `postgres` / `postgres`

Схемы баз данных создаются автоматически благодаря настройке:  
`spring.jpa.hibernate.ddl-auto=update`.
---