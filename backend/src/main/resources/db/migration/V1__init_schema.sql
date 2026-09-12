-- V1__init_schema.sql
-- Initial schema for the Road Cutting Permission service.
-- This is the single source of truth for the DB shape; Hibernate ddl-auto is set to "validate" only.

-- pgcrypto gives us gen_random_uuid() for primary keys.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ---------------------------------------------------------------------------
-- application: one row per permission application.
-- ---------------------------------------------------------------------------
CREATE TABLE application (
    -- Internal surrogate key. Never exposed to clients; the public identifier is application_number.
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Human-readable, globally-displayed number, e.g. DDN-RCP-000123-2026-27. Unique across all tenants.
    application_number          VARCHAR(64) NOT NULL UNIQUE,

    -- Tenant (city) this application belongs to. Every query in the app must filter by this column.
    tenant_id                   VARCHAR(64) NOT NULL,

    -- Road-type code (BT / CC / WBM / KUTCHA), validated against rates.json at write time.
    road_type                   VARCHAR(32) NOT NULL,

    length_in_meters            NUMERIC(10, 2) NOT NULL,
    width_in_meters             NUMERIC(10, 2) NOT NULL,
    -- Whole square metres, ceil(length * width). Stored so history/audits don't depend on recomputation.
    area_in_sqm                 NUMERIC(10, 0) NOT NULL,
    duration_in_days            INTEGER NOT NULL,

    applicant_type              VARCHAR(32) NOT NULL,
    proposed_start_date         DATE NOT NULL,
    -- The date the application was created; used as "applicationDate" in the fee-calculation rules.
    application_date            DATE NOT NULL,

    -- Fee breakdown, persisted at creation time and never recomputed from a client-sent value afterwards.
    restoration_charge          NUMERIC(14, 2) NOT NULL,
    permission_fee              NUMERIC(14, 2) NOT NULL,
    urgency_surcharge           NUMERIC(14, 2) NOT NULL,
    security_deposit            NUMERIC(14, 2) NOT NULL,
    total_amount                NUMERIC(14, 2) NOT NULL,

    status                      VARCHAR(32) NOT NULL,

    -- Identity of the applicant, taken from RequestInfo.userInfo at creation time (no login system exists).
    applicant_uuid              VARCHAR(128) NOT NULL,
    applicant_mobile_number     VARCHAR(32) NOT NULL,

    -- Optional client-supplied idempotency key (stretch-goal friendly; unused by the Core flow but harmless to keep).
    request_reference_id        VARCHAR(128),

    -- Standard audit columns required by the spec.
    created_by                  VARCHAR(128) NOT NULL,
    created_time                TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_by            VARCHAR(128) NOT NULL,
    last_modified_time          TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Optimistic-locking column used by JPA's @Version to guard against concurrent workflow actions.
    version                     BIGINT NOT NULL DEFAULT 0
);

-- Every tenant-scoped lookup filters by tenant_id, so it is the leading column in this composite index.
CREATE INDEX idx_application_tenant_status ON application (tenant_id, status);
CREATE INDEX idx_application_tenant_applicant ON application (tenant_id, applicant_uuid);
CREATE INDEX idx_application_tenant_mobile ON application (tenant_id, applicant_mobile_number);
-- application_number is already UNIQUE (and therefore indexed), but tenant-scoped lookups still filter tenant_id too.
CREATE INDEX idx_application_tenant_number ON application (tenant_id, application_number);

-- ---------------------------------------------------------------------------
-- transition_history: append-only log of every workflow transition (including the initial CREATE).
-- ---------------------------------------------------------------------------
CREATE TABLE transition_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- FK to application.id. We keep tenant_id here too (denormalised) so history queries stay tenant-scoped
    -- without always needing a join back to application.
    application_id      UUID NOT NULL REFERENCES application (id),
    tenant_id           VARCHAR(64) NOT NULL,

    -- Action performed: CREATE, VERIFY, SEND_BACK, APPROVE, REJECT, CANCEL.
    action              VARCHAR(32) NOT NULL,

    previous_state      VARCHAR(32),
    resulting_state      VARCHAR(32) NOT NULL,

    actor_uuid          VARCHAR(128) NOT NULL,
    actor_role          VARCHAR(32) NOT NULL,

    comment             VARCHAR(1024),

    created_time        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_transition_history_application ON transition_history (application_id, created_time);
CREATE INDEX idx_transition_history_tenant ON transition_history (tenant_id);

-- ---------------------------------------------------------------------------
-- application_sequence: one row per (tenant_id, financial_year), used to hand out
-- gap-free, collision-free sequence numbers for application numbers under concurrent load.
-- ---------------------------------------------------------------------------
CREATE TABLE application_sequence (
    tenant_id           VARCHAR(64) NOT NULL,
    financial_year      VARCHAR(16) NOT NULL,
    last_value          BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant_id, financial_year)
);
