# LexHive

LexHive is a book-lending REST API for a small library. Librarians manage the catalogue and members; members browse books, borrow them, and return them. Borrowing rules (loan limits, due dates, overdue blocking) are enforced on the server, and every endpoint is protected with JWT-based authentication and role-based access.

## Features

- **Books** – create, list, view, update, and delete books, with copy counts that stay in sync with loans.
- **Members** – create, list, view, update, and delete library members.
- **Loans** – borrow and return books, list loans, view a single loan.
- **Authentication** – log in with a username and password and receive a signed JWT.
- **Two roles** – `LIBRARIAN` (full access) and `MEMBER` (can only see and act on their own loans).
- **Borrowing rules** – a member can hold at most 3 active loans, each loan lasts 14 days, and a member with an overdue loan cannot borrow again until it is returned.
- **Data safety** – row-level locks on borrow/return so concurrent requests can't over-lend a book, plus database constraints as a second line of defence.
- **Consistent errors** – every error response has the same JSON shape with a machine-readable code.
- **Observability** – health checks, custom loan metrics, and a request ID attached to every log line and response.



## Tech stack


| Area                 | Technology                                            |
| -------------------- | ----------------------------------------------------- |
| Language             | Java 17                                               |
| Framework            | Spring Boot 4 (Web MVC, Validation, Actuator)         |
| Security             | Spring Security, OAuth2 Resource Server, JWT (HS256)  |
| Persistence          | PostgreSQL, Spring JDBC                               |
| Local infrastructure | Docker Compose (started automatically by Spring Boot) |
| Build                | Maven (wrapper included)                              |




## Getting started



### Prerequisites

- **JDK 17 or newer**
- **Docker** (Docker Desktop or any Docker engine) – used to run PostgreSQL

You do not need to install Maven or PostgreSQL yourself.

### Run the application

```bash
git clone <your-repo-url>
cd LexHive
./mvnw spring-boot:run
```
if there is an error something like "permission denied while trying to connect to the docker."
run the below command. This will create a group which can access docker same as root/sudo.

```bash
sudo addgroup --system docker
sudo adduser $USER docker
sudo snap disable docker
sudo snap enable docker
```

On startup Spring Boot runs `compose.yaml`, which starts a PostgreSQL container named `lexhive-postgres` on port `5434`. The first time the container starts, `docker/postgres/init.sql` creates the tables and seeds two members.

The API is available at `http://localhost:8080`. Check that it's up:

```bash
curl http://localhost:8080/actuator/health
# {"groups":["liveness","readiness"],"status":"UP"}
```

> If you have several JDKs installed, make sure `JAVA_HOME` points to Java 17+. On macOS: `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`.



### Reset the database

The database lives in a Docker volume, so data survives restarts. To start over with a fresh, seeded database:

```bash
docker compose down -v
```

`init.sql` only runs against an empty volume, so you also need this after any change to the schema (for example, the `password_hash` column on `members`).

### Run the tests

```bash
./mvnw test
```



## Demo accounts

These accounts are available out of the box:


| Username                | Password       | Role      | Linked member ID |
| ----------------------- | -------------- | --------- | ---------------- |
| `librarian`             | `librarian123` | LIBRARIAN | –                |
| `siddharth@lexhive.com` | `siddharth123` | MEMBER    | 1                |
| `varshney@lexhive.com`  | `varshney123`  | MEMBER    | 2                |


The librarian account is defined in code. Members are stored in the `members` table with a BCrypt-hashed password, so the two seeded members and any member a librarian creates through `POST /api/members` log in the same way: their email is the username, and the password is the one set when the member was created.

## Authentication

Every endpoint except login, `/actuator/health` and `/actuator/info` needs a token. Get one from `POST /api/auth/login` (see [Auth APIs](#auth-apis)) and send it on every request:

```
Authorization: Bearer <accessToken>
```

Tokens expire after 60 minutes. For members, the token carries a `memberId` claim; the server always uses that value to decide whose loans a member can see or create, so a member cannot act on behalf of someone else.

### Using Postman

The examples below use two Postman variables, `{{librarianToken}}` and `{{memberToken}}`, so you never have to copy tokens by hand. You can paste any curl command into Postman (**Import → Raw text**) and it becomes a request.

1. Create a Postman environment (for example `LexHive Local`) and select it.
2. Create the login request: `POST http://localhost:8080/api/auth/login` with a raw JSON body.
3. add the token in collection variables so that it can be used again in subsequent APIs.
4. Send the login request once as the librarian and once as a member:
  ```json
   {"username":"librarian","password":"librarian123"}
  ```

Both tokens are now saved in the PostMan collection. Logging in as a different member (for example `varshney@lexhive.com`), token will need to be replaced in variables such that `memberToken` would point to new values.

## API reference



### Endpoints and access


| Method | Path                         | Who can call it                    | Description                                         |
| ------ | ---------------------------- | ---------------------------------- | --------------------------------------------------- |
| POST   | `/api/auth/login`            | Anyone                             | Log in and receive a JWT                            |
| GET    | `/api/books`                 | Librarian, Member                  | List all books                                      |
| GET    | `/api/books/{id}`            | Librarian, Member                  | Get one book                                        |
| POST   | `/api/books`                 | Librarian                          | Create a book                                       |
| PUT    | `/api/books/{id}`            | Librarian                          | Update a book                                       |
| DELETE | `/api/books/{id}`            | Librarian                          | Delete a book (only if it has no loan history)      |
| GET    | `/api/members`               | Librarian                          | List all members                                    |
| GET    | `/api/members/{id}`          | Librarian                          | Get one member                                      |
| POST   | `/api/members`               | Librarian                          | Create a member                                     |
| PUT    | `/api/members/{id}`          | Librarian                          | Update a member                                     |
| DELETE | `/api/members/{id}`          | Librarian                          | Delete a member (only if they have no loan history) |
| POST   | `/api/loans`                 | Librarian, Member                  | Borrow a book                                       |
| GET    | `/api/loans`                 | Librarian, Member                  | List loans (members only see their own)             |
| GET    | `/api/loans/{id}`            | Librarian, Member (own loans only) | Get one loan                                        |
| POST   | `/api/loans/{id}/return`     | Librarian, Member (own loans only) | Return a borrowed book                              |
| GET    | `/actuator/health`           | Anyone (details for Librarian)     | Health check                                        |
| GET    | `/actuator/health/liveness`  | Anyone                             | Liveness probe                                      |
| GET    | `/actuator/health/readiness` | Anyone                             | Readiness probe                                     |
| GET    | `/actuator/info`             | Anyone                             | Application info                                    |
| GET    | `/actuator/metrics`          | Librarian                          | List available metrics                              |
| GET    | `/actuator/metrics/{name}`   | Librarian                          | Read one metric                                     |


Create endpoints return `201 Created` with a `Location` header pointing to the new resource. Delete endpoints return `204 No Content`.

The IDs in the examples (book `1`, member `3`, loan `1`) assume a fresh database where you run the requests in order. Replace them with the IDs from your own responses.

---



### Auth APIs



#### Log in as the librarian

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"librarian","password":"librarian123"}'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2026-10-18T17:13:46Z",
  "roles": ["LIBRARIAN"],
  "memberId": null
}
```



#### Log in as a member (siddharth)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"siddharth@lexhive.com","password":"siddharth123"}'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresAt": "2026-10-18T17:13:46Z",
  "roles": ["MEMBER"],
  "memberId": 1
}
```



#### Log in as a member (varshney)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"varshney@lexhive.com","password":"varshney123"}'
```

A wrong username or password returns `401`:

```json
{"status":401,"error":"INVALID_CREDENTIALS","message":"Username or password is incorrect"}
```

---



### Book APIs

**Request body** for create and update:


| Field         | Type    | Rules                                  |
| ------------- | ------- | -------------------------------------- |
| `title`       | string  | required, max 255 chars                |
| `author`      | string  | required, max 255 chars                |
| `isbn`        | string  | required, max 17 chars, must be unique |
| `totalCopies` | integer | required, 0 or more                    |


`availableCopies` is managed by the server: it goes down when a book is borrowed and up when it is returned.

#### Create a book (librarian)

```bash
curl -X POST http://localhost:8080/api/books \
  -H "Authorization: Bearer {{librarianToken}}" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Clean Code","author":"Robert C. Martin","isbn":"9780132350884","totalCopies":2}'
```

```json
{"id":1,"title":"Clean Code","author":"Robert C. Martin","isbn":"9780132350884","totalCopies":2,"availableCopies":2}
```



#### List all books (librarian or member)

```bash
curl http://localhost:8080/api/books \
  -H "Authorization: Bearer {{memberToken}}"
```

```json
[{"id":1,"title":"Clean Code","author":"Robert C. Martin","isbn":"9780132350884","totalCopies":2,"availableCopies":2}]
```



#### Get one book (librarian or member)

```bash
curl http://localhost:8080/api/books/1 \
  -H "Authorization: Bearer {{memberToken}}"
```



#### Update a book (librarian)

All fields are required, so send the full book. `totalCopies` can't be set lower than the number of copies currently on loan (`409 INSUFFICIENT_TOTAL_COPIES`).

```bash
curl -X PUT http://localhost:8080/api/books/1 \
  -H "Authorization: Bearer {{librarianToken}}" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Clean Code (2nd Ed)","author":"Robert C. Martin","isbn":"9780132350884","totalCopies":4}'
```



#### Delete a book (librarian)

Returns `204`. A book that has ever been borrowed can't be deleted (`409 BOOK_IN_USE`).

```bash
curl -X DELETE http://localhost:8080/api/books/1 \
  -H "Authorization: Bearer {{librarianToken}}"
```

---



### Member APIs

All member endpoints are librarian-only.

**Request body**:


| Field      | Type   | Rules                                                  |
| ---------- | ------ | ------------------------------------------------------ |
| `name`     | string | required, max 255 chars                                |
| `email`    | string | required, valid email, max 255 chars, must be unique   |
| `password` | string | required on create, 8–72 chars; not accepted on update |


The password is stored as a BCrypt hash and is never returned.

#### Create a member

```bash
curl -X POST http://localhost:8080/api/members \
  -H "Authorization: Bearer {{librarianToken}}" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Asha","email":"asha@lexhive.com","password":"asha-secret"}'
```

```json
{"id":3,"name":"Asha","email":"asha@lexhive.com"}
```

The new member can log in straight away with `{"username":"asha@lexhive.com","password":"asha-secret"}`.

#### List all members

```bash
curl http://localhost:8080/api/members \
  -H "Authorization: Bearer {{librarianToken}}"
```

```json
[{"id":1,"name":"siddharth","email":"siddharth@lexhive.com"},{"id":2,"name":"varshney","email":"varshney@lexhive.com"}]
```



#### Get one member

```bash
curl http://localhost:8080/api/members/3 \
  -H "Authorization: Bearer {{librarianToken}}"
```



#### Update a member

Only `name` and `email` can be changed.

```bash
curl -X PUT http://localhost:8080/api/members/3 \
  -H "Authorization: Bearer {{librarianToken}}" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Asha Rao","email":"asha.rao@lexhive.com"}'
```

```json
{"id":3,"name":"Asha Rao","email":"asha.rao@lexhive.com"}
```



#### Delete a member

Returns `204`. A member who has ever borrowed a book can't be deleted (`409 MEMBER_IN_USE`).

```bash
curl -X DELETE http://localhost:8080/api/members/3 \
  -H "Authorization: Bearer {{librarianToken}}"
```

---



### Loan APIs

**Request body** for borrowing:


| Field      | Type   | Rules                                                               |
| ---------- | ------ | ------------------------------------------------------------------- |
| `bookId`   | number | required                                                            |
| `memberId` | number | required for librarians; ignored for members (taken from the token) |


A loan looks like this:

```json
{"id":1,"bookId":1,"memberId":1,"borrowedAt":"2026-10-18T16:13:46Z","dueDate":"2026-11-01T16:13:46Z","returnedAt":null}
```



#### Borrow a book as a member (for yourself)

```bash
curl -X POST http://localhost:8080/api/loans \
  -H "Authorization: Bearer {{memberToken}}" \
  -H 'Content-Type: application/json' \
  -d '{"bookId":1}'
```



#### Borrow a book as the librarian (on behalf of a member)

```bash
curl -X POST http://localhost:8080/api/loans \
  -H "Authorization: Bearer {{librarianToken}}" \
  -H 'Content-Type: application/json' \
  -d '{"bookId":1,"memberId":2}'
```



#### List your own loans (member)

Members always get only their own loans; a `memberId` query parameter is ignored.

```bash
curl http://localhost:8080/api/loans \
  -H "Authorization: Bearer {{memberToken}}"
```



#### List all loans (librarian)

```bash
curl http://localhost:8080/api/loans \
  -H "Authorization: Bearer {{librarianToken}}"
```



#### List one member's loans (librarian)

```bash
curl "http://localhost:8080/api/loans?memberId=1" \
  -H "Authorization: Bearer {{librarianToken}}"
```



#### Get one loan (member, own loans only)

Asking for another member's loan returns `403 NOT_YOUR_LOAN`.

```bash
curl http://localhost:8080/api/loans/1 \
  -H "Authorization: Bearer {{memberToken}}"
```



#### Get one loan (librarian)

```bash
curl http://localhost:8080/api/loans/1 \
  -H "Authorization: Bearer {{librarianToken}}"
```



#### Return a book (member, own loans only)

The response is the loan with `returnedAt` filled in. Returning the same loan twice gives `409 LOAN_ALREADY_RETURNED`.

```bash
curl -X POST http://localhost:8080/api/loans/1/return \
  -H "Authorization: Bearer {{memberToken}}"
```



#### Return a book (librarian, any loan)

```bash
curl -X POST http://localhost:8080/api/loans/2/return \
  -H "Authorization: Bearer {{librarianToken}}"
```

---



### Actuator APIs (health, info, metrics)



#### Health (public)

```bash
curl http://localhost:8080/actuator/health
```

```json
{"groups":["liveness","readiness"],"status":"UP"}
```



#### Health with details (librarian)

Shows the status of each component, such as the database and disk space.

```bash
curl http://localhost:8080/actuator/health \
  -H "Authorization: Bearer {{librarianToken}}"
```



#### Liveness probe (public)

```bash
curl http://localhost:8080/actuator/health/liveness
```



#### Readiness probe (public)

```bash
curl http://localhost:8080/actuator/health/readiness
```



#### Application info (public)

```bash
curl http://localhost:8080/actuator/info
```

```json
{"app":{"name":"LexHive","description":"Book lending service"}}
```



#### List available metrics (librarian)

```bash
curl http://localhost:8080/actuator/metrics \
  -H "Authorization: Bearer {{librarianToken}}"
```



#### Books borrowed counter (librarian)

```bash
curl http://localhost:8080/actuator/metrics/lexhive.loans.borrowed \
  -H "Authorization: Bearer {{librarianToken}}"
```

```json
{"availableTags":[],"measurements":[{"statistic":"COUNT","value":4.0}],"name":"lexhive.loans.borrowed"}
```



#### Books returned counter (librarian)

```bash
curl http://localhost:8080/actuator/metrics/lexhive.loans.returned \
  -H "Authorization: Bearer {{librarianToken}}"
```



## Borrowing rules


| Rule                                         | Default | Error when broken           |
| -------------------------------------------- | ------- | --------------------------- |
| Maximum active (unreturned) loans per member | 3       | `409 MAX_ACTIVE_LOANS`      |
| Loan duration                                | 14 days | –                           |
| Members with an overdue loan cannot borrow   | –       | `409 OVERDUE_LOANS`         |
| A book needs at least one available copy     | –       | `409 NO_COPIES_AVAILABLE`   |
| A loan can only be returned once             | –       | `409 LOAN_ALREADY_RETURNED` |


The limits are configurable in `application.yml`:

```yaml
lexhive:
  borrowing:
    max-active-loans: 3
    loan-duration-days: 14
```



## Error responses

Every error uses the same format:

```json
{"status":409,"error":"NO_COPIES_AVAILABLE","message":"No copies of this book are available"}
```


| Status | Code                                                                                | When                                                                 |
| ------ | ----------------------------------------------------------------------------------- | -------------------------------------------------------------------- |
| 400    | `VALIDATION_ERROR`                                                                  | A field is missing or invalid                                        |
| 400    | `MALFORMED_REQUEST`                                                                 | The body is missing or isn't valid JSON                              |
| 400    | `INVALID_PARAMETER`                                                                 | A path or query parameter has the wrong type (e.g. `/api/books/abc`) |
| 401    | `INVALID_CREDENTIALS`                                                               | Wrong username or password                                           |
| 401    | –                                                                                   | Missing, expired, or invalid token                                   |
| 403    | –                                                                                   | Your role isn't allowed to call this endpoint                        |
| 403    | `NOT_YOUR_LOAN`                                                                     | A member tried to view or return someone else's loan                 |
| 404    | `BOOK_NOT_FOUND`, `MEMBER_NOT_FOUND`, `LOAN_NOT_FOUND`                              | The resource doesn't exist                                           |
| 409    | `DUPLICATE_ISBN`, `DUPLICATE_EMAIL`                                                 | A unique value is already taken                                      |
| 409    | `BOOK_IN_USE`, `MEMBER_IN_USE`                                                      | The record has loan history and can't be deleted                     |
| 409    | `INSUFFICIENT_TOTAL_COPIES`                                                         | `totalCopies` would drop below the number on loan                    |
| 409    | `NO_COPIES_AVAILABLE`, `MAX_ACTIVE_LOANS`, `OVERDUE_LOANS`, `LOAN_ALREADY_RETURNED` | A borrowing rule was broken                                          |




## Observability

- **Health** – `/actuator/health` is public and reports `UP`/`DOWN`. Librarians also see component details (database, disk space). Liveness and readiness probes are available for container orchestrators.
- **Metrics** – two custom counters track business activity: `lexhive.loans.borrowed` and `lexhive.loans.returned`. See [Actuator APIs](#actuator-apis-health-info-metrics) for the curls.
- **Request IDs** – every response includes a `request_id` header, and every log line written while handling that request is tagged with the same ID, so you can trace one request through the logs:
  ```
  INFO [449831df] ... LoanService : Member 1 borrowed book 2 as loan 3
  ```



## Project structure

```
src/main/java/com/example/LexHive/booklender
├── book/            Book entity, controller, service, repository, DTOs
├── member/          Member entity, controller, service, repository, DTOs
├── loan/            Loan entity, borrowing policy, controller, service, repository
├── security/        Login endpoint, JWT creation, password hashing, security configuration
├── exception/       Shared error type and global exception handler
├── observability/   Request logging filter (request IDs, timings)
└── config/          Borrowing settings and the application clock

docker/postgres/init.sql   Database schema and seed data
compose.yaml               Local PostgreSQL container
```



## Configuration

Main settings in `src/main/resources/application.yml`:


| Property                                | Default                                    | Purpose                  |
| --------------------------------------- | ------------------------------------------ | ------------------------ |
| `spring.datasource.url`                 | `jdbc:postgresql://localhost:5434/lexhive` | Database connection      |
| `lexhive.borrowing.max-active-loans`    | `3`                                        | Loan limit per member    |
| `lexhive.borrowing.loan-duration-days`  | `14`                                       | Days until a loan is due |
| `lexhive.security.jwt-secret`           | demo value                                 | Key used to sign JWTs    |
| `lexhive.security.token-expiry-minutes` | `60`                                       | Token lifetime           |


Any property can be overridden with an environment variable, for example `LEXHIVE_SECURITY_JWT_SECRET=...`.

## Known limitations

This project is built for learning and demonstration. Before using it in production you would want to:

- Move the librarian account out of code and into the database, and add a way for members to change their password.
- Supply the JWT secret from an environment variable or secrets manager rather than `application.yml`.
- Add pagination to the list endpoints.
- Manage the schema with a migration tool such as Flyway or Liquibase.

