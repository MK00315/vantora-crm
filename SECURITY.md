# Security policy

## Reporting a vulnerability

Do not publish exploitable security findings in a public issue. Contact the repository owner privately with:

- the affected endpoint or component;
- clear reproduction steps;
- the security impact;
- any suggested mitigation; and
- whether the report contains sensitive data.

Please allow time to reproduce, fix, and release the issue before public disclosure. This portfolio repository does not currently promise a formal response SLA or bug bounty.

## Supported versions

Only the latest version on the default branch is supported. Historical commits and personal forks may contain known issues.

## Security expectations

- Never commit `.env`, tokens, database dumps, cloud keys, or real CRM data.
- Generate local `.env` files with `scripts/init-env.ps1`, never commit them, and inject a separate JWT secret from a managed secret store in every deployed environment.
- Serve the application over HTTPS and set secure cookies in non-local environments.
- Restrict CORS, database networking, Redis networking, and object-storage permissions.
- Do not deploy the local Mailpit inbox; use a monitored transactional mail provider.
- Expose the backend only through a trusted proxy that replaces untrusted forwarding headers.
- Rate-limit authentication and high-cost search endpoints at the ingress layer.
- Keep Java, Node.js, container base images, and dependencies patched.
- Back up PostgreSQL, test restores, and define retention appropriate for customer data.
- Forward application and infrastructure logs to monitored, access-controlled storage.

For the system-specific threat analysis, see [docs/security.md](docs/security.md).
