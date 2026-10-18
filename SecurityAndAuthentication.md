# Security & Authentication — JWT

## 1. Objective

Secure the Book Lending Microservice using JWT-based authentication and role-based authorization.

The service will use predefined users and roles configured in the application. A full identity provider or user-registration system is not required.

**Technology:** Java 17+, Spring Boot, Spring Security, JWT, BCrypt.

## 2. User Roles

Two roles will be configured:

| Role | Permissions |
|---|---|
| `MEMBER` | View books, borrow books, return books |
| `LIBRARIAN` | View, add, update, and delete books; manage library members |

These permissions will be enforced at the API level using Spring Security.

## 3. Authentication Flow

1. The client sends credentials to `POST /auth/login`.
2. The server validates the credentials against predefined users.
3. If valid, the server generates a signed JWT containing the username, role, issue time, and expiration time.
4. The server returns the JWT to the client.
5. The client includes the token in the `Authorization` header of subsequent requests.
6. Spring Security validates the token and establishes the authenticated principal.
7. Authorization rules determine whether the user has permission to access the requested resource.

### Login Request

`POST /auth/login`

```json
{
  "username": "alice",
  "password": "password123"
}
```

### Login Response

```json
{
  "accessToken": "<signed-jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

The credentials above are illustrative. Actual configured credentials must be documented separately for local testing.

### Authenticated API Request

`GET /api/books`

```http
Authorization: Bearer <signed-jwt>
```

## 4. JWT Structure and Validation

The JWT will contain the following claims:

- `sub`: authenticated username
- `role`: assigned role
- `iat`: token issue time
- `exp`: token expiration time

The server will sign tokens using a securely configured signing key.

For every authenticated request, the security filter will:

1. Extract the Bearer token from the `Authorization` header.
2. Validate the token's signature and expiration.
3. Extract the username and role from the validated claims.
4. Establish the authenticated principal in the Spring Security context.
5. Allow the request to proceed to authorization checks.

Missing, malformed, expired, or invalid tokens will be rejected.

## 5. Authorization Rules

The following access rules will be configured:

| Endpoint | Required access |
|---|---|
| `POST /auth/login` | Public |
| `GET /api/books/**` | Authenticated user |
| `POST /api/books` | `LIBRARIAN` |
| `PUT /api/books/**` | `LIBRARIAN` |
| `DELETE /api/books/**` | `LIBRARIAN` |
| `POST /api/loans/borrow` | `MEMBER` |
| `POST /api/loans/return` | `MEMBER` |
| `POST /api/members` | `LIBRARIAN` |

All endpoints will require authentication unless explicitly declared public.

The exact endpoint paths may be adjusted to match the implemented API.

## 6. Predefined Users

A small set of users will be configured in application configuration or through an in-memory user store.

Example:

| Username | Role |
|---|---|
| `alice` | `MEMBER` |
| `librarian` | `LIBRARIAN` |

Passwords will be stored as BCrypt hashes rather than plaintext. Production secrets and passwords must not be committed to version control.

## 7. Error Handling

| Scenario | HTTP status |
|---|---|
| Missing authentication token | `401 Unauthorized` |
| Invalid or expired JWT | `401 Unauthorized` |
| Authenticated user lacks permission | `403 Forbidden` |
| Invalid login credentials | `401 Unauthorized` |

Error responses will use a consistent JSON format without exposing passwords, signing keys, or sensitive token details.

## 8. Security Considerations

- Use BCrypt for password hashing.
- Keep JWT signing keys outside source control.
- Configure token expiration, for example, one hour.
- Validate token signatures and expiration on every authenticated request.
- Use HTTPS in deployed environments.
- Do not log passwords or complete JWTs.
- Apply least-privilege role permissions.
- Do not trust client-supplied roles; authorization must rely on validated server-issued claims.
- Do not store sensitive personal information in JWT claims.
- For this assignment, refresh tokens and logout-based token revocation are out of scope unless explicitly required.

## 9. Testing

Automated tests will cover:

- Successful login returns a valid JWT.
- Invalid credentials are rejected.
- Public login endpoint is accessible without a token.
- Protected endpoints reject missing or invalid tokens.
- Expired tokens are rejected.
- Members can access permitted operations.
- Members cannot perform librarian-only operations.
- Librarians can perform authorized catalog-management operations.
- Unauthorized requests return the expected HTTP status codes.

## 10. Implementation Components

The implementation will use the following components:

- `SecurityConfig`: Defines authentication and authorization rules.
- `UserDetailsService`: Loads predefined users and their roles.
- `AuthController`: Exposes the login endpoint.
- `JwtService`: Generates and validates JWTs.
- `JwtAuthenticationFilter`: Extracts and validates Bearer tokens.
- `PasswordEncoder`: Verifies BCrypt password hashes.
- Spring Security exception handlers: Return consistent `401` and `403` responses.

## 11. Scope

This implementation provides lightweight, stateless authentication and role-based authorization suitable for the assignment. It does not require a database-backed user-management system or an external identity provider.

Book, member, and loan data will continue to be persisted in the relational database independently of the authentication configuration.