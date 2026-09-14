# Contributing to Vantora CRM

Thank you for improving Vantora CRM. Small, reviewable changes with tests are easier to understand and safer to release.

## Local workflow

1. Create a branch from the current default branch.
2. Run `.\scripts\init-env.ps1` to create an ignored `.env` with a unique local JWT secret; never commit it.
3. Start PostgreSQL, Redis, and Mailpit with `docker compose -f compose.dev.yml up -d`.
4. Point the backend to PostgreSQL `localhost:5433`, Redis `localhost:6380`, and SMTP `localhost:1025`; set `JWT_SECRET` to the generated value from `.env`, then run `mvn spring-boot:run` from `backend/`.
5. Run the client with `npm run dev` from `frontend/`.
6. Add or update tests with the implementation.
7. Run the checks below before opening a pull request.

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

## Engineering conventions

- Keep controller methods thin and business rules in application services.
- Resolve tenant identity from the authenticated principal; never trust a tenant ID from a request body or query parameter.
- Scope every tenant-owned read, update, and delete query by both record ID and tenant ID.
- Use DTOs at API boundaries rather than exposing JPA entities.
- Add a Flyway migration for schema changes. Never edit an already-released migration.
- Return the shared error response shape and avoid leaking implementation details.
- Keep React data fetching in query/mutation hooks and forms in React Hook Form schemas.
- Include loading, empty, error, keyboard, and responsive states for UI work.
- Use meaningful commit messages; do not commit generated builds, local `.env` files, credentials, or customer data.

## Pull-request checklist

- [ ] The change is limited to one clear purpose.
- [ ] Tenant isolation and authorization were considered.
- [ ] Tests cover success, validation, and forbidden paths where applicable.
- [ ] Backend verification, frontend lint/test/build, and Compose validation pass.
- [ ] API or configuration changes are documented.
- [ ] Registration/authentication changes cover the verification-before-login contract.
- [ ] Logs and screenshots contain no secrets or personal data.
- [ ] Database migrations are backward-safe for a rolling deployment, or the deployment constraint is documented.
