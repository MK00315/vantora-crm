CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(140) NOT NULL,
    slug VARCHAR(80) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_last_served_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_tenants_reminder_fairness ON tenants(reminder_last_served_at, id);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    email_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    session_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_users_role CHECK (role IN ('SUPER_ADMIN','COMPANY_ADMIN','HR','RECRUITER','EMPLOYEE')),
    CONSTRAINT ck_users_status CHECK (status IN ('INVITED','ACTIVE','SUSPENDED'))
);
CREATE INDEX idx_users_tenant_name ON users(tenant_id, last_name, first_name);
CREATE INDEX idx_users_tenant_status ON users(tenant_id, status);

CREATE TABLE pending_registrations (
    id UUID PRIMARY KEY,
    company_name VARCHAR(140) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    token_expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_pending_registrations_created ON pending_registrations(created_at);
CREATE INDEX idx_pending_registrations_token_expiry ON pending_registrations(token_expires_at);

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(40),
    company VARCHAR(160),
    website VARCHAR(300),
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    owner_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_customer_status CHECK (status IN ('PROSPECT','ACTIVE','INACTIVE'))
);
CREATE INDEX idx_customers_tenant_created ON customers(tenant_id, created_at DESC);
CREATE INDEX idx_customers_tenant_status ON customers(tenant_id, status);
CREATE INDEX idx_customers_tenant_name ON customers(tenant_id, lower(name));

CREATE TABLE leads (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(40),
    company VARCHAR(160),
    title VARCHAR(120),
    source VARCHAR(80),
    stage VARCHAR(30) NOT NULL,
    estimated_value NUMERIC(14,2) NOT NULL DEFAULT 0,
    owner_id UUID REFERENCES users(id) ON DELETE SET NULL,
    notes TEXT,
    stage_changed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_lead_stage CHECK (stage IN ('NEW','CONTACTED','QUALIFIED','PROPOSAL','NEGOTIATION','WON','LOST'))
);
CREATE INDEX idx_leads_tenant_stage ON leads(tenant_id, stage);
CREATE INDEX idx_leads_tenant_created ON leads(tenant_id, created_at DESC);
CREATE INDEX idx_leads_tenant_owner ON leads(tenant_id, owner_id);

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    due_at TIMESTAMPTZ,
    assignee_id UUID REFERENCES users(id) ON DELETE SET NULL,
    lead_id UUID REFERENCES leads(id) ON DELETE SET NULL,
    completed_at TIMESTAMPTZ,
    reminder_sent_at TIMESTAMPTZ,
    reminder_next_attempt_at TIMESTAMPTZ,
    reminder_claimed_at TIMESTAMPTZ,
    reminder_claim_id UUID,
    reminder_attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_task_status CHECK (status IN ('TODO','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT ck_task_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT'))
);
CREATE INDEX idx_tasks_tenant_due ON tasks(tenant_id, due_at) WHERE status NOT IN ('COMPLETED','CANCELLED');
CREATE INDEX idx_tasks_tenant_assignee ON tasks(tenant_id, assignee_id);
CREATE INDEX idx_tasks_reminder_queue ON tasks(due_at, reminder_next_attempt_at)
    WHERE reminder_sent_at IS NULL AND status NOT IN ('COMPLETED','CANCELLED');

CREATE TABLE activities (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id UUID,
    summary VARCHAR(300) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_activities_tenant_created ON activities(tenant_id, created_at DESC);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_hash VARCHAR(64),
    user_agent VARCHAR(300),
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_refresh_tokens_user_active ON refresh_tokens(user_id, expires_at) WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_tokens_family_active ON refresh_tokens(family_id, expires_at) WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked_at) WHERE revoked_at IS NOT NULL;

CREATE TABLE action_tokens (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_action_token_type CHECK (type IN ('PASSWORD_RESET','EMAIL_VERIFICATION'))
);
CREATE INDEX idx_action_tokens_user_type ON action_tokens(user_id, type, expires_at);
CREATE INDEX idx_action_tokens_expiry ON action_tokens(expires_at);
CREATE INDEX idx_action_tokens_used ON action_tokens(used_at) WHERE used_at IS NOT NULL;

CREATE TABLE stored_files (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    object_key VARCHAR(420) NOT NULL UNIQUE,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    deleted_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_stored_file_size CHECK (size_bytes > 0),
    CONSTRAINT ck_stored_file_status CHECK (status IN ('PENDING','ACTIVE','FAILED','DELETED'))
);
CREATE INDEX idx_stored_files_tenant_created ON stored_files(tenant_id, created_at DESC);
CREATE INDEX idx_stored_files_active_quota ON stored_files(tenant_id) WHERE deleted_at IS NULL;

CREATE TABLE storage_quota_lock (
    id INTEGER PRIMARY KEY
);
INSERT INTO storage_quota_lock (id) VALUES (1);

CREATE TABLE deployment_quota_lock (
    id INTEGER PRIMARY KEY
);
INSERT INTO deployment_quota_lock (id) VALUES (1);

CREATE TABLE shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
