# Training Platform

`training-platform` - это платформа управления обучением с backend на Spring Boot и frontend на React.

## Стек

- Backend: Java 21, Spring Boot 4, Spring MVC, Spring Data JPA, Flyway, Quartz, Spring Security
- Frontend: React, TypeScript, Vite, Ant Design, TanStack Query
- База данных: PostgreSQL 16

## Структура репозитория

- `src/` - backend-код и тесты
- `frontend/` - frontend-приложение
- `demo-data/` - демонстрационные данные, вспомогательные сценарии и скрипты для локальной проверки
- `.mvn/`, `mvnw`, `mvnw.cmd` - Maven Wrapper для локальной сборки
- `docker-compose.yml` - локальный PostgreSQL для разработки

## Требования

- Java 21
- Node.js и npm
- Docker Desktop или совместимый Docker Engine

## Локальный запуск

### 1. Запуск PostgreSQL

```powershell
docker compose up -d
```

Параметры локальной базы данных по умолчанию:

- база данных: `training_platform`
- пользователь: `postgres`
- пароль: `postgres`
- порт: `5433`

### 2. Запуск backend

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Backend по умолчанию доступен по адресу `http://localhost:8080`.

Профиль `dev` поддерживает локальное переключение демонстрационного пользователя через заголовок `X-Demo-Actor-Id`.

### 3. Запуск frontend

```powershell
cd frontend
Copy-Item .env.example .env -Force
npm install
npm run dev
```

Frontend по умолчанию доступен по адресу `http://localhost:5173`.

Пример локального файла `.env`:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_DEMO_ACTOR_ID=1
```

## Сборка

### Backend

```powershell
.\mvnw.cmd -DskipTests compile
```

### Frontend

```powershell
cd frontend
npm run build
```

## Что есть в приложении

- личный кабинет обучающегося
- назначенное обучение и самостоятельное тестирование
- просмотр результатов и уведомлений
- управленческая аналитика
- инструменты эксперта для работы с учебным контентом
- административные разделы для пользователей, оргструктуры, доступов, назначений, импорта и аудита

## Примечания

- `frontend/.env` используется только локально и не должен попадать в Git
- `docker-compose.yml` содержит настройки только для локальной разработки
- `demo-data/` предназначен для локального наполнения и сценарной проверки приложения
- если описание в `README` расходится с текущим кодом, ориентироваться следует на реализацию в `src/` и `frontend/src/`
