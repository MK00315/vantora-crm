# ADR 0001: Shared-schema, row-scoped multi-tenancy

- Status: Accepted
- Date: 2026-08-24
- Decision owners: Project maintainers

## Context

Vantora CRM must give each organization a private workspace while remaining simple enough to run locally and deploy as a portfolio-scale SaaS. The main alternatives were a database per tenant, a schema per tenant, and a shared schema with a tenant discriminator on each owned row.

Database-per-tenant offers the strongest physical separation but multiplies provisioning, migrations, connection pools, backups, and support work. Schema-per-tenant reduces some infrastructure cost but still complicates migrations and query routing. Both are unnecessary for the present scale and make local development harder.

## Decision

Use one PostgreSQL schema. Every tenant-owned table contains a non-null `tenant_id` foreign key. The authenticated principal supplies the tenant scope, and repository operations query by both tenant and resource identity.

The following rules are mandatory:

1. The server never treats a request body's tenant ID, a query parameter, or an unverified header as authorization.
2. Every tenant-owned lookup, count, update, and delete includes `tenant_id`.
3. New tenant tables include tenant-first composite indexes for common query shapes.
4. Cross-tenant administration is not added implicitly; it requires a separately designed privileged control plane.
5. Tenant-isolation tests create at least two tenants and prove reads and mutations cannot cross the boundary.
6. Logs may include a tenant identifier for correlation but must not include tokens, passwords, or sensitive CRM fields.

## Consequences

### Positive

- One Flyway migration path and one connection pool.
- Straightforward local Docker setup and lower operational cost.
- Efficient per-tenant queries with appropriate indexes.
- Transactions can safely span multiple CRM modules for one tenant.

### Negative

- A missing tenant predicate can become a severe data-isolation bug.
- Noisy tenants share database compute and storage.
- Tenant-specific backup and restore is an application-level operation.
- Regulatory requirements may eventually demand stronger physical isolation.

## Mitigations

- Centralize authenticated tenant context and use tenant-aware repository methods.
- Review query code specifically for tenant predicates.
- Maintain two-tenant integration tests for every new owned resource.
- Consider PostgreSQL Row-Level Security as defense in depth after its effects on migrations, connection pooling, and tests are understood.
- Instrument query latency and tenant-level usage so noisy workloads are visible.

## Revisit when

A customer requires dedicated infrastructure, the system needs tenant-level restore guarantees, regulatory controls require physical isolation, or measured noisy-neighbor problems cannot be solved with quotas and indexing.
