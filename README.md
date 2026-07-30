# Todolist API

A production-ready REST API for managing a todolist, built with **Spring Boot 4.1 (Java 21)**,
**PostgreSQL**, and **Flyway** migrations. It ships with a multi-stage **Dockerfile** and a
**docker-compose** stack (API + PostgreSQL + Nginx reverse proxy) so it can be deployed to a
VPS with a single command.

## Features

- Full CRUD for todos (`create`, `read`, `update`, `partial update`, `delete`)
- Filtering by completion status and priority, plus full-text search on title/description
- Pagination and sorting on any field
- Bean validation with consistent JSON error responses
- Database schema managed by Flyway migrations (no `ddl-auto=update` in production)
- Health endpoint via Spring Boot Actuator (`/actuator/health`)
- Runs as a non-root user in a slim JRE image
- Nginx reverse proxy in front of the app

## Tech stack

| Layer        | Choice                         |
|--------------|--------------------------------|
| Language     | Java 21                        |
| Framework    | Spring Boot 4.1 (Web MVC, Data JPA) |
| Database     | PostgreSQL 16                  |
| Migrations   | Flyway                         |
| Build        | Maven                          |
| Packaging    | Docker (multi-stage)           |
| Reverse proxy| Nginx                          |

## Data model

A `todo` has the following fields:

| Field         | Type      | Notes                                   |
|---------------|-----------|-----------------------------------------|
| `id`          | number    | Auto-generated                          |
| `title`       | string    | Required, max 255 chars                 |
| `description` | string    | Optional                                |
| `completed`   | boolean   | Defaults to `false`                     |
| `priority`    | enum      | `LOW`, `MEDIUM` (default), `HIGH`       |
| `dueDate`     | date      | Optional (`YYYY-MM-DD`)                 |
| `createdAt`   | timestamp | Set automatically                       |
| `updatedAt`   | timestamp | Updated automatically                   |

## API reference

Base path: `/api/todos`

| Method   | Path              | Description                          | Success code |
|----------|-------------------|--------------------------------------|--------------|
| `GET`    | `/api/todos`      | List todos (filter/search/paginate)  | 200          |
| `GET`    | `/api/todos/{id}` | Get a single todo                    | 200          |
| `POST`   | `/api/todos`      | Create a todo                        | 201          |
| `PUT`    | `/api/todos/{id}` | Replace a todo                       | 200          |
| `PATCH`  | `/api/todos/{id}` | Partially update a todo              | 200          |
| `DELETE` | `/api/todos/{id}` | Delete a todo                        | 204          |

### List query parameters

| Param       | Default     | Description                                            |
|-------------|-------------|-------------------------------------------------------|
| `completed` | –           | `true` / `false`                                      |
| `priority`  | –           | `LOW` / `MEDIUM` / `HIGH`                              |
| `search`    | –           | Case-insensitive text match on title/description      |
| `page`      | `0`         | Zero-based page index                                 |
| `size`      | `20`        | Page size (1–100)                                     |
| `sort`      | `createdAt` | `id`, `title`, `completed`, `priority`, `dueDate`, `createdAt`, `updatedAt` |
| `direction` | `desc`      | `asc` / `desc`                                        |

### Example

```bash
curl -X POST http://localhost/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy milk","priority":"HIGH","dueDate":"2026-08-15"}'
```

See [`requests.http`](./requests.http) for more ready-to-run examples.

---

## Running locally (development)

You need Java 21 and a PostgreSQL instance. The quickest way to get a database:

```bash
docker run --name todolist-pg -e POSTGRES_DB=todolist \
  -e POSTGRES_USER=todolist -e POSTGRES_PASSWORD=todolist \
  -p 5432:5432 -d postgres:16-alpine
```

Then run the app (defaults already point to `localhost:5432`):

```bash
./mvnw spring-boot:run
```

The API is now on `http://localhost:8080`.

Run the tests (in-memory H2, no external database needed):

```bash
./mvnw test
```

---

## Deploying to a VPS with Docker Compose

### 1. Prerequisites on the VPS

- A Linux VPS (Ubuntu/Debian recommended) with a public IP
- Docker Engine + Docker Compose plugin installed:

```bash
curl -fsSL https://get.docker.com | sh
```

### 2. Get the code onto the server

```bash
git clone <your-repo-url> my-app
cd my-app
```

(or copy the project directory to the server with `scp`/`rsync`).

### 3. Configure environment variables

```bash
cp .env.example .env
nano .env          # set a strong POSTGRES_PASSWORD, adjust HTTP_PORT / CORS if needed
```

At minimum, set `POSTGRES_PASSWORD`. It is required — the stack refuses to start without it.

### 4. Build and start the stack

```bash
docker compose up -d --build
```

This will:

1. Build the application jar inside a Maven container (Java compilation happens here).
2. Start PostgreSQL with a persistent named volume (`db-data`).
3. Run Flyway migrations automatically on first boot to create the schema.
4. Start the API and an Nginx reverse proxy.

### 5. Verify

```bash
docker compose ps
curl http://localhost/actuator/health      # -> {"status":"UP"}
curl http://localhost/api/todos            # -> paged (empty) list
```

The API is reachable on `http://<your-vps-ip>/` (port 80 via Nginx).

### Common operations

```bash
docker compose logs -f app        # tail application logs
docker compose restart app        # restart just the API
docker compose down               # stop everything (keeps the DB volume)
docker compose down -v            # stop AND delete the database volume
docker compose up -d --build      # rebuild after code changes
```

### Updating after code changes

```bash
git pull
docker compose up -d --build
```

Flyway applies any new migrations on startup.

---

## Production hardening (recommended next steps)

- **HTTPS**: put a TLS certificate in front. Easiest options are adding
  [Caddy](https://caddyserver.com/) instead of Nginx (automatic Let's Encrypt),
  or running Certbot and mounting the certs into the Nginx container, then adding
  a `listen 443 ssl;` server block.
- **Firewall**: allow only ports 22, 80, 443 (`ufw allow 80,443,22/tcp`).
  PostgreSQL is not published to the host — it stays on the internal Docker network.
- **Backups**: schedule `pg_dump` of the `db` container, e.g.
  `docker compose exec db pg_dump -U todolist todolist > backup.sql`.
- **CORS**: set `APP_CORS_ALLOWED_ORIGINS` to your real frontend domain instead of `*`.
- **Secrets**: keep `.env` out of version control (already in `.gitignore`).

---

## Project structure

```
my-app/
├── src/main/java/com/example/myapp/
│   ├── config/         # CORS / web configuration
│   ├── controller/     # REST endpoints
│   ├── dto/            # request / response records
│   ├── entity/         # JPA entities
│   ├── exception/      # error handling
│   ├── repository/     # Spring Data JPA repositories
│   └── service/        # business logic
├── src/main/resources/
│   ├── db/migration/   # Flyway SQL migrations
│   ├── application.properties
│   └── application-prod.properties
├── nginx/default.conf  # reverse proxy config
├── Dockerfile          # multi-stage build
├── docker-compose.yml  # app + postgres + nginx
├── .env.example        # copy to .env
└── requests.http       # sample API calls
```
