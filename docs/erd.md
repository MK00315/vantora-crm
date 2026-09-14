# Data model

The schema uses UUID-style identifiers, UTC timestamps, foreign keys, and explicit indexes. Exact column details live in the Flyway migrations; this diagram communicates ownership and cardinality.

```mermaid
erDiagram
    TENANTS ||--o{ USERS : contains
    PENDING_REGISTRATIONS {
        uuid id PK
        string email UK
        string password_hash
        string company_name
        string first_name
        string last_name
        string token_hash UK
        timestamp token_expires_at
        timestamp created_at
        timestamp updated_at
    }
    TENANTS ||--o{ REFRESH_TOKENS : scopes
    USERS ||--o{ REFRESH_TOKENS : owns
    TENANTS ||--o{ ACTION_TOKENS : scopes
    USERS ||--o{ ACTION_TOKENS : owns
    TENANTS ||--o{ LEADS : owns
    USERS ||--o{ LEADS : assigned_to
    TENANTS ||--o{ CUSTOMERS : owns
    USERS ||--o{ CUSTOMERS : manages
    TENANTS ||--o{ TASKS : owns
    USERS ||--o{ TASKS : assigned_to
    LEADS ||--o{ TASKS : relates_to
    TENANTS ||--o{ ACTIVITIES : records
    USERS ||--o{ ACTIVITIES : performs
    TENANTS ||--o{ STORED_FILES : owns

    TENANTS {
        uuid id PK
        string name
        string slug UK
        boolean active
        timestamp reminder_last_served_at
        timestamp created_at
        timestamp updated_at
    }

    USERS {
        uuid id PK
        uuid tenant_id FK
        string email
        string password_hash
        string first_name
        string last_name
        string role
        string status
        bigint session_version
        timestamp email_verified_at
        timestamp created_at
        timestamp updated_at
    }

    REFRESH_TOKENS {
        uuid id PK
        uuid tenant_id FK
        uuid user_id FK
        uuid family_id
        string token_hash UK
        timestamp expires_at
        timestamp revoked_at
        string replaced_by_hash
        timestamp created_at
    }

    ACTION_TOKENS {
        uuid id PK
        uuid tenant_id FK
        uuid user_id FK
        string token_hash UK
        string type
        timestamp expires_at
        timestamp used_at
        timestamp created_at
    }

    LEADS {
        uuid id PK
        uuid tenant_id FK
        uuid owner_id FK
        string first_name
        string last_name
        string email
        string company
        string stage
        string source
        decimal estimated_value
        timestamp stage_changed_at
        timestamp created_at
        timestamp updated_at
    }

    CUSTOMERS {
        uuid id PK
        uuid tenant_id FK
        uuid owner_id FK
        string name
        string email
        string company
        string phone
        string status
        timestamp created_at
        timestamp updated_at
    }

    TASKS {
        uuid id PK
        uuid tenant_id FK
        uuid assignee_id FK
        uuid lead_id FK
        string title
        string status
        string priority
        timestamp due_at
        timestamp completed_at
        timestamp reminder_sent_at
        timestamp reminder_next_attempt_at
        timestamp reminder_claimed_at
        uuid reminder_claim_id
        int reminder_attempts
        timestamp created_at
        timestamp updated_at
    }

    ACTIVITIES {
        uuid id PK
        uuid tenant_id FK
        uuid actor_id FK
        string action
        string entity_type
        uuid entity_id
        string summary
        timestamp created_at
    }

    STORED_FILES {
        uuid id PK
        uuid tenant_id FK
        string object_key UK
        string filename
        string content_type
        bigint size_bytes
        string status
        timestamp deleted_at
        timestamp created_at
        timestamp updated_at
    }
```

## Data rules

- User email is globally unique because email is the platform login identity; CRM contact email fields are not authentication identities.
- Lead, customer, and task indexes begin with `tenant_id` and then the fields used for filtering or ordering.
- Deletes should preserve auditability where the product needs history; prefer status transitions or soft deletion for customer records after defining retention rules.
- Money values use fixed-precision decimals and an explicit currency policy.
- Application timestamps use UTC; the frontend formats them in the user's locale.
- Refresh tokens contain no recoverable secret in the database—only a digest suitable for lookup and comparison.
- Pending registrations have no tenant relationship: tenant and user rows are created only after a single-use mailbox-verification token is consumed.
- Refresh records carry a family ID and fixed absolute family expiry so replay containment revokes one compromised session chain without terminating independent device sessions or creating immortal sliding sessions.
- User session versions invalidate outstanding access and refresh credentials immediately after password, role, or status changes.
- Stored-file metadata moves through `PENDING`, `ACTIVE`, `FAILED`, and `DELETED` states. Quota calculations include reservations, while downloads require an active tenant-owned row.
- The singleton `storage_quota_lock` row serializes short global quota reservations, and the `shedlock` table coordinates scheduled work across API instances.
