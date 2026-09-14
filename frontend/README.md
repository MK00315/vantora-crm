# Vantora CRM frontend

React 19 + TypeScript client for the multi-tenant CRM. It includes authenticated workspace routing, dashboard analytics, lead Kanban and list views, customer management and CSV export, tasks, team administration, profile/security, settings, responsive navigation, and loading/error/empty states.

## Run locally

```bash
npm install
npm run dev
```

Vite listens on `http://localhost:5173` and proxies `/api` to `http://localhost:8080` during development. Copy `.env.example` to `.env` only when you need to override defaults.

## Environment

- `VITE_API_BASE_URL`: defaults to `/api/v1`.
- `VITE_DEMO_MODE`: defaults to `false`. Set to `true` only for an intentional UI demo without the API; demo records are in-memory and reset on reload.

## Quality checks

```bash
npm run lint
npm run test
npm run build
```

Authentication keeps the short-lived access token in memory. The server owns the refresh token in an HttpOnly cookie, and the Axios interceptor rotates it with a single shared refresh request after a 401.
