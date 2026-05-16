# FinTrackerPro

Финансовый трекер для учёта доходов и расходов: категории, бюджеты, отчёты и управление пользователями.

## Стек технологий

- Java 17
- Spring Boot 3
- PostgreSQL 14
- Maven
- Spring Security (JWT, Google OAuth2)
- Flyway (миграции БД)
- Docker / Docker Compose (опционально)

## Требования

- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Git
- (опционально) Docker и Docker Compose

## Деплой на Render (Google OAuth)

В **Environment** сервиса на Render обязательно задайте:

| Переменная | Значение |
|------------|----------|
| `GOOGLE_CLIENT_ID` | тот же ID, что `REACT_APP_GOOGLE_CLIENT_ID` на Vercel |
| `GOOGLE_CLIENT_SECRET` | из Google Cloud Console |
| `FRONTEND_URL` | `https://fintrackerpro.vercel.app` |

Проверка после деплоя: `GET https://<ваш-бэк>/api/auth/config` → `"googleOAuthConfigured": true`.

На **Vercel** для фронта: `REACT_APP_API_BASE_URL` **оставьте пустым** (запросы идут на `/api` через прокси в `vercel.json`).

## Быстрый старт

```bash
git clone https://github.com/mikhaylov2001/FinTrackerPro.git
cd FinTrackerPro
cp .env.example .env
mvn spring-boot:run