# ADR 0002: Short-lived access tokens with rotating refresh-token cookies

- Status: Accepted
- Date: 2026-08-24
- Decision owners: Project maintainers

## Context

The browser client needs a smooth signed-in experience without giving long-lived bearer credentials to JavaScript. A fully server-side session is viable, but the project also demonstrates stateless API authorization and needs a clean path for native or external clients.

Storing both JWTs in local storage is simple but lets successful script injection read the long-lived refresh credential. A non-rotating refresh token also gives an attacker a longer replay window.

## Decision

Use two credentials:

- a short-lived signed JWT access token returned in the JSON response and held in application memory; and
- a longer-lived opaque refresh token sent only as an `HttpOnly`, `SameSite` cookie and stored as a one-way hash in PostgreSQL.

On refresh, the server validates the cookie, expiry, revocation state, user session version, verified user status, and tenant status. It revokes the old database record and issues a new token and cookie in the same logical operation. Every initial login creates a distinct session-family ID and one absolute family expiry, both retained as that token rotates, so repeated refreshes cannot extend a session forever. Reuse of a rotated token revokes only the compromised family; a stale token from that family is retained long enough for replay detection without being able to terminate independent sessions. Logout revokes the current refresh token and clears the cookie.

Each access JWT also carries the user's current session version. The authentication filter checks it, the current role, user status, verification state, and tenant state against the database on every protected request. Password, role, and status changes increment the session version and revoke refresh sessions, invalidating already-issued access tokens immediately.

The refresh endpoint must use explicit CORS credentials policy and origin validation. Production cookies use `Secure`; local HTTP development may disable only that flag through an environment profile.

## Consequences

### Positive

- Most API authorization remains stateless and horizontally scalable.
- JavaScript cannot directly read the long-lived credential.
- Rotation reduces the useful replay window and family-scoped replay handling contains one compromised device without creating a cross-device denial of service.
- Sessions can be listed and revoked later without changing the access-token format.

### Negative

- The client needs coordinated `401` retry logic.
- Refresh records add database writes and cleanup work.
- Cookies introduce CSRF considerations on the refresh/logout endpoints.
- Every protected request performs a small current-user and tenant lookup so immediate revocation is not fully stateless.

## Required controls

- Sign access tokens with a strong secret or asymmetric key and validate algorithm, issuer, expiry, user, and tenant claims.
- Hash refresh tokens at rest; never log raw tokens or cookies.
- Rotate tokens atomically, retain a stable family ID and fixed absolute family expiry, and detect attempts to reuse a rotated token without letting arbitrary historical tokens revoke unrelated families.
- Limit sessions by user/device if abuse becomes a concern.
- Rate-limit login, registration, verification/resend, password reset, refresh, and anonymous logout at the edge.
- Use HTTPS, `Secure`, `HttpOnly`, and an appropriate `SameSite` policy in production.
- Keep the access-token lifetime short and make both lifetimes configurable.
- Clean up expired/revoked refresh records on a schedule.

## Alternatives considered

- **Both tokens in local storage:** rejected because it exposes the durable credential to browser JavaScript.
- **Long-lived JWT only:** rejected because revocation and replay containment are weak.
- **Server session cookie only:** valid, but not selected because the API is designed to demonstrate bearer-token authorization for multiple clients.
