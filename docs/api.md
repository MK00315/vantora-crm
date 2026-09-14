# API overview

With the full Docker stack, use the same-origin API root `http://localhost:3000/api/v1`. Nginx proxies those requests to the backend, which is intentionally not published on a host port. Swagger UI is at `http://localhost:3000/swagger-ui.html` and OpenAPI JSON is at `http://localhost:3000/v3/api-docs`.

When the backend runs directly from an IDE, its default root is `http://localhost:8080/api/v1`.

## Authentication

Send the in-memory access token on protected requests:

```http
Authorization: Bearer <access-token>
Content-Type: application/json
```

Access tokens are returned only by login and refresh. The refresh token is never placed in JSON; the server sends it as the `mtcrm_refresh` HttpOnly cookie scoped to `/api/v1/auth`. Browser or Postman refresh and logout requests must include that cookie.

### Register and verify a workspace

```http
POST /api/v1/auth/register
```

```json
{
  "companyName": "Vantora Demo",
  "firstName": "Maya",
  "lastName": "Rao",
  "email": "admin@vantora.local",
  "password": "Vantora123!"
}
```

The password must be 10–72 characters and contain uppercase, lowercase, and numeric characters. A successful registration stores an expiring pending signup, sends a verification message, and returns `201 Created`. It does not create a tenant, user, cookie, or access token. Consuming the single-use verification token atomically creates the tenant and its first verified `COMPANY_ADMIN`:

```json
{
  "message": "Check your email. Verify the signup to create your workspace"
}
```

Registration deliberately does not create an authenticated session. In local Docker development, open Mailpit at `http://localhost:8025`, open the newest message, and either follow its link or copy the `token` value from the URL fragment (`#token=...`) into this request. Fragments are used so reverse proxies and access logs never receive the bearer secret:

```http
POST /api/v1/auth/verify-email
Content-Type: application/json
```

```json
{
  "token": "<verification-token>"
}
```

If the verification token expires, request a replacement. The response is intentionally generic so account existence is not disclosed:

```http
POST /api/v1/auth/resend-verification
Content-Type: application/json
```

```json
{
  "email": "admin@vantora.local"
}
```

### Login, refresh, and logout

Login succeeds only after email verification:

```http
POST /api/v1/auth/login
```

```json
{
  "email": "admin@vantora.local",
  "password": "Vantora123!"
}
```

```http
POST /api/v1/auth/refresh
Cookie: mtcrm_refresh=<opaque-token>
```

Refresh rotates the cookie and returns a new access token. Refresh records belong to one session family: reuse of a rotated token revokes that compromised family without terminating unrelated sessions on other devices.

```http
POST /api/v1/auth/logout
Cookie: mtcrm_refresh=<opaque-token>
```

Logout revokes the current refresh session, expires the cookie, and returns a message response.

## Endpoint map

All routes below begin with `/api/v1`. `Authenticated` means an active, verified user in an active tenant unless a narrower role is listed.

### Sessions

| Method | Route | Access | Purpose |
| --- | --- | --- | --- |
| `POST` | `/auth/register` | Public, rate-limited | Create an expiring pending signup and return a `201` message; no tenant or session yet |
| `POST` | `/auth/resend-verification` | Public, rate-limited | Send a replacement verification token without revealing account existence |
| `POST` | `/auth/verify-email` | Public, rate-limited | Consume a single-use token and atomically create the tenant and verified company administrator |
| `POST` | `/auth/login` | Public, rate-limited | Authenticate a verified user and start a refresh session |
| `POST` | `/auth/refresh` | Refresh cookie, rate-limited | Rotate the refresh token and issue an access token |
| `POST` | `/auth/logout` | Refresh cookie | Revoke the session and clear the cookie |
| `GET` | `/auth/me` | Authenticated | Return the current user and tenant summary |
| `POST` | `/auth/forgot-password` | Public, rate-limited | Request a reset without revealing account existence |
| `POST` | `/auth/reset-password` | Public, rate-limited | Consume a single-use token, replace the password, and revoke sessions |

### Leads

| Method | Route | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/leads` | Authenticated | Search, filter, sort, and page leads |
| `GET` | `/leads/{id}` | Authenticated | Get a tenant-owned lead |
| `POST` | `/leads` | `SUPER_ADMIN`, `COMPANY_ADMIN`, `HR`, `RECRUITER` | Create a lead |
| `PUT` | `/leads/{id}` | `SUPER_ADMIN`, `COMPANY_ADMIN`, `HR`, `RECRUITER` | Replace editable lead fields |
| `PATCH` | `/leads/{id}/stage` | `SUPER_ADMIN`, `COMPANY_ADMIN`, `HR`, `RECRUITER` | Move a lead to another pipeline stage |
| `DELETE` | `/leads/{id}` | `SUPER_ADMIN`, `COMPANY_ADMIN`, `HR` | Delete a lead |

Lead stages are `NEW`, `CONTACTED`, `QUALIFIED`, `PROPOSAL`, `NEGOTIATION`, `WON`, and `LOST`.

Example create body:

```json
{
  "firstName": "Aarav",
  "lastName": "Shah",
  "email": "aarav@example.com",
  "phone": "+91 98765 43210",
  "company": "Orbit Labs",
  "title": "Engineering Director",
  "source": "Referral",
  "stage": "NEW",
  "estimatedValue": 125000,
  "ownerId": "08957386-02a7-4db1-a55c-9be226b439cb",
  "notes": "Interested in a multi-team rollout."
}
```

### Customers

| Method | Route | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/customers` | Authenticated | Search, filter, sort, and page customers |
| `GET` | `/customers/{id}` | Authenticated | Get a tenant-owned customer |
| `POST` | `/customers` | `SUPER_ADMIN`, `COMPANY_ADMIN`, `HR`, `RECRUITER` | Create a customer |
| `PUT` | `/customers/{id}` | `SUPER_ADMIN`, `COMPANY_ADMIN`, `HR`, `RECRUITER` | Replace editable customer fields |
| `DELETE` | `/customers/{id}` | `SUPER_ADMIN`, `COMPANY_ADMIN`, `HR` | Delete a customer |
| `GET` | `/customers/export` | Authenticated | Download the tenant's customers as CSV |

Customer statuses are `PROSPECT`, `ACTIVE`, and `INACTIVE`. Website values must use `http` or `https`; active-content schemes such as `javascript:` are rejected.

### Tasks

| Method | Route | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/tasks` | Authenticated | Search, filter, sort, and page tasks |
| `GET` | `/tasks/{id}` | Authenticated | Get a tenant-owned task |
| `POST` | `/tasks` | Authenticated | Create a task |
| `PUT` | `/tasks/{id}` | Authenticated | Replace editable task fields |
| `PATCH` | `/tasks/{id}/status` | Authenticated | Change task status |
| `DELETE` | `/tasks/{id}` | `SUPER_ADMIN`, `COMPANY_ADMIN`, `HR`, `RECRUITER` | Delete a task |

Task statuses are `TODO`, `IN_PROGRESS`, `COMPLETED`, and `CANCELLED`. Priorities are `LOW`, `MEDIUM`, `HIGH`, and `URGENT`. Dates use ISO 8601 UTC, for example `2027-01-15T10:30:00Z`.

### Team, dashboard, activity, and files

| Method | Route | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/users` | Authenticated | Search, filter, sort, and page tenant users |
| `GET` | `/users/{id}` | Authenticated | Get a tenant user |
| `POST` | `/users` | `SUPER_ADMIN`, `COMPANY_ADMIN` | Invite a tenant user |
| `PUT` | `/users/{id}` | `SUPER_ADMIN`, `COMPANY_ADMIN` | Change allowed name, role, or status fields |
| `DELETE` | `/users/{id}` | `SUPER_ADMIN`, `COMPANY_ADMIN` | Soft-suspend a tenant user |
| `PUT` | `/users/me` | Authenticated | Change the current user's first and last name |
| `PUT` | `/users/me/password` | Authenticated | Change the password and revoke existing sessions |
| `GET` | `/dashboard` | Authenticated | Return Redis-cached tenant metrics and upcoming work |
| `GET` | `/activities` | Authenticated | Page the tenant's recent activity feed |
| `POST` | `/files` | Authenticated | Upload one multipart field named `file` |
| `GET` | `/files?page=0&size=20` | Authenticated | Page the tenant's active file metadata |
| `GET` | `/files/{id}` | Authenticated | Get metadata and a current download URL |
| `GET` | `/files/content/{tenantId}/{filename}` | Authenticated owner tenant | Read an active local-development object |
| `DELETE` | `/files?key=...` | `SUPER_ADMIN`, `COMPANY_ADMIN`, `HR`, `RECRUITER` | Delete an active object by its tenant-owned key |

Tenant roles are `COMPANY_ADMIN`, `HR`, `RECRUITER`, and `EMPLOYEE`; user statuses are `INVITED`, `ACTIVE`, and `SUSPENDED`. `SUPER_ADMIN` is reserved for platform-controlled identities and cannot be assigned through tenant-facing invitations.

Uploads accept JPEG, PNG, WebP, PDF, and CSV content up to 10 MB. The server enforces content validation, per-tenant and global byte/file/hourly quotas, and bounded concurrent writes. A short database reservation is created before storage I/O; failed or abandoned reservations are cleaned safely. Local storage returns an authenticated application URL, while S3 downloads use a fresh, short-lived presigned URL for a private object.

## List queries

Lead, customer, task, user, activity, and file collections use zero-based pagination. Most CRM resources also support search, filters, and allowlisted sorting.

| Parameter | Meaning |
| --- | --- |
| `q` | Case-insensitive free-text search over resource-specific fields |
| `page` | Zero-based page number; default `0` |
| `size` | Items per page, from `1` to `100` |
| `sort` | Resource-specific allowlisted sort field |
| `direction` | `asc` or `desc` |

Omit optional enum filters rather than sending an empty value.

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

## Errors

Validation, authentication, authorization, conflicts, and missing resources use one safe response shape:

```json
{
  "timestamp": "2026-08-24T12:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Please correct the highlighted fields",
  "path": "/api/v1/leads",
  "traceId": "2d652b1b6f694a5e",
  "violations": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ]
}
```

Common statuses:

- `400` malformed input, invalid pagination, validation failure, or an exceeded application quota;
- `401` missing, invalid, expired, suspended, or not-yet-verified authentication;
- `403` valid identity without the required permission;
- `404` missing resource in the authenticated tenant;
- `409` uniqueness or protected-state conflict; and
- `429` authentication rate limit exceeded.

The `X-Request-Id` response header and `traceId` error field support log correlation. They are not credentials.

## Tenant behavior

Clients do not send a tenant header or select a tenant in CRM request bodies. The API derives tenant identity from the verified access token and scopes resource, relationship, aggregate, cache, and storage operations to it. A UUID or object key from another tenant behaves as unavailable and never reveals the other tenant's data.

For runnable examples, import the [Postman collection](../postman/MT-CRM.postman_collection.json) and [local environment](../postman/Local.postman_environment.json).
