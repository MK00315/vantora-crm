# Test strategy

Testing follows the risk profile of a multi-tenant CRM: identity and isolation failures matter more than cosmetic coverage percentages. A change is not considered safe only because its happy path compiles.

## Test layers

| Layer | Focus | Tooling |
| --- | --- | --- |
| Backend unit | Verification, token-family rotation, service rules, protected roles, cache invalidation, quotas, reminder claiming/retry | JUnit 5, Mockito, Spring Security Test |
| Backend slice/integration | Request validation, auth responses, JPA mappings, Flyway-compatible behavior, two-tenant repository isolation | Spring Boot Test, MockMvc, H2 test profile |
| Frontend component | Forms, protected routing, query states, user actions, accessible labels | Vitest, Testing Library, jsdom |
| Static/build | Java compilation, TypeScript strictness, ESLint, production bundling | Maven, TypeScript, ESLint, Vite |
| Container integration | Both images build; full Compose starts; Flyway runs on PostgreSQL; Nginx, API, Mailpit, and health probes respond | Docker, Docker Compose, curl, PostgreSQL client |
| Manual smoke | Verification/cookie flow, responsive UI, browser navigation, PostgreSQL/Redis/Mailpit/storage integration | Docker environment, browser, Postman |

## Required tenant-isolation cases

For each tenant-owned resource, tests should establish tenant A and tenant B and verify:

1. A list from tenant A contains no tenant B records.
2. Tenant A cannot read a known tenant B UUID.
3. Tenant A cannot update, change status/stage, or delete a tenant B UUID.
4. Tenant A cannot assign its record to a tenant B user or lead.
5. Aggregates, exports, activity feeds, cache keys, and file paths remain tenant-scoped.
6. A privileged role remains privileged only inside its authenticated tenant.

Returning `404` for an out-of-tenant record is preferred where it prevents an identifier-existence oracle.

## Authentication cases

- Registration creates only an expiring pending record, returns `201 MessageResponse`, and does not issue a session; verification atomically creates the tenant and verified company administrator.
- Login and refresh reject an unverified user; verification is single-use and resend responses do not reveal account existence.
- Password and email constraints return field-level errors.
- Login does not distinguish a missing account from a bad password.
- Access tokens reject the wrong signature, issuer, or expired claims.
- Refresh rotates the stored digest and rejects reuse; replay revokes only the compromised session family, not unrelated devices.
- Logout revokes the refresh token and clears the cookie.
- Suspended users and inactive tenants cannot refresh.
- Password reset consumes its action token, invalidates siblings, and revokes existing sessions; an invited user becomes verified when completing the onboarding reset.
- Authentication throttling covers exact sensitive paths, remains memory-bounded, and returns `429` plus a retry hint after the configured window is exceeded.

## Storage and background-job cases

- Upload validation rejects disallowed content and files above the request limit.
- Per-tenant and global byte/file/hourly quotas count pending reservations and cannot be bypassed by deletion.
- Storage I/O occurs outside the short global quota-lock transaction; failed writes become failed metadata and stale reservations are cleaned.
- File list, detail, content, and delete operations require the authenticated tenant and active metadata.
- Reminder eligibility requires an active tenant and active, verified assignee.
- One scheduler instance claims a bounded, persisted round-robin batch across workspaces (including a greater-than-50-tenant regression case), SMTP runs outside the claim transaction, successful reminders are marked once, and failures back off without starving later tasks.

## Local commands

```powershell
Set-Location backend
mvn clean verify

Set-Location ..\frontend
npm ci
npm run lint
npm test
npm run build

Set-Location ..
$env:JWT_SECRET = 'local-check-only-signing-key-0123456789abcdef0123456789abcdef'
docker compose config --quiet
docker compose -f compose.dev.yml config --quiet
```

## Manual smoke path

1. Generate `.env`, start the full Docker stack, and verify the Nginx and proxied API health endpoints.
2. Register workspace A and verify registration returns `201` without an access token or refresh cookie.
3. Confirm login is rejected before verification, open the Mailpit message, verify the address, and then sign in.
4. Create lead/customer/task data and upload a file larger than 1 MB but smaller than 10 MB through Nginx; record the resource identifiers.
5. Register and verify workspace B in a private browser session.
6. Attempt to use workspace A's UUIDs, file key, and file URL from workspace B; verify no data is disclosed.
7. Move a lead stage and task status; verify dashboard values and activity update.
8. Reload to exercise refresh-cookie bootstrap, then log out and verify protected calls return `401`.
9. Check narrow/mobile and desktop layouts, keyboard navigation, validation, empty states, and API failure states.

The Postman examples support the API portion of this walkthrough. CI additionally boots the Compose stack on PostgreSQL 17, verifies Flyway and same-origin Swagger routing, then executes mailbox verification, refresh rotation, protected CRUD, streaming CSV, image storage, cross-tenant denial, and edge-rate-limit checks before teardown.
