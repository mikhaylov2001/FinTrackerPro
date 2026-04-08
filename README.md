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

## Быстрый старт

```bash
git clone https://github.com/mikhaylov2001/FinTrackerPro.git
cd FinTrackerPro
cp .env .env
mvn spring-boot:run