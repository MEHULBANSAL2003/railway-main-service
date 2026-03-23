-- ============================================================================
-- V2: Date-based activation/deactivation with period tables
-- Replaces boolean is_active with temporal period tracking
-- ============================================================================

-- ── 1. Create period tables ────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS railway_main.train_periods (
  period_id      BIGSERIAL    PRIMARY KEY,
  train_id       BIGINT       NOT NULL REFERENCES railway_main.trains(train_id),
  effective_from DATE         NOT NULL,
  effective_till DATE,
  reason         VARCHAR(500),
  created_by     BIGINT,
  created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
  CONSTRAINT uk_train_period UNIQUE (train_id, effective_from)
);
CREATE INDEX IF NOT EXISTS idx_train_period_lookup
  ON railway_main.train_periods(train_id, effective_from, effective_till);

CREATE TABLE IF NOT EXISTS railway_main.train_type_periods (
  period_id      BIGSERIAL    PRIMARY KEY,
  type_id        BIGINT       NOT NULL REFERENCES railway_main.train_types(type_id),
  effective_from DATE         NOT NULL,
  effective_till DATE,
  reason         VARCHAR(500),
  created_by     BIGINT,
  created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
  CONSTRAINT uk_train_type_period UNIQUE (type_id, effective_from)
);
CREATE INDEX IF NOT EXISTS idx_train_type_period_lookup
  ON railway_main.train_type_periods(type_id, effective_from, effective_till);

CREATE TABLE IF NOT EXISTS railway_main.coach_type_periods (
  period_id      BIGSERIAL    PRIMARY KEY,
  type_id        BIGINT       NOT NULL REFERENCES railway_main.coach_types(type_id),
  effective_from DATE         NOT NULL,
  effective_till DATE,
  reason         VARCHAR(500),
  created_by     BIGINT,
  created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
  CONSTRAINT uk_coach_type_period UNIQUE (type_id, effective_from)
);
CREATE INDEX IF NOT EXISTS idx_coach_type_period_lookup
  ON railway_main.coach_type_periods(type_id, effective_from, effective_till);

CREATE TABLE IF NOT EXISTS railway_main.quota_periods (
  period_id      BIGSERIAL    PRIMARY KEY,
  quota_id       BIGINT       NOT NULL REFERENCES railway_main.quotas(quota_id),
  effective_from DATE         NOT NULL,
  effective_till DATE,
  reason         VARCHAR(500),
  created_by     BIGINT,
  created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
  CONSTRAINT uk_quota_period UNIQUE (quota_id, effective_from)
);
CREATE INDEX IF NOT EXISTS idx_quota_period_lookup
  ON railway_main.quota_periods(quota_id, effective_from, effective_till);

-- ── 2. Migrate existing data: seed initial periods from is_active ──────────

-- Trains: active trains get an open period, inactive get a closed one
INSERT INTO railway_main.train_periods (train_id, effective_from, effective_till, reason, created_by, created_at)
SELECT train_id,
       COALESCE(created_at::date, CURRENT_DATE),
       CASE WHEN is_active = true THEN NULL ELSE (CURRENT_DATE - INTERVAL '1 day')::date END,
       CASE WHEN is_active = true THEN 'Migration — was active' ELSE 'Migration — was inactive' END,
       created_by,
       NOW()
FROM railway_main.trains;

-- Train types
INSERT INTO railway_main.train_type_periods (type_id, effective_from, effective_till, reason, created_by, created_at)
SELECT type_id,
       COALESCE(created_at::date, CURRENT_DATE),
       CASE WHEN is_active = true THEN NULL ELSE (CURRENT_DATE - INTERVAL '1 day')::date END,
       CASE WHEN is_active = true THEN 'Migration — was active' ELSE 'Migration — was inactive' END,
       created_by,
       NOW()
FROM railway_main.train_types;

-- Coach types
INSERT INTO railway_main.coach_type_periods (type_id, effective_from, effective_till, reason, created_by, created_at)
SELECT type_id,
       COALESCE(created_at::date, CURRENT_DATE),
       CASE WHEN is_active = true THEN NULL ELSE (CURRENT_DATE - INTERVAL '1 day')::date END,
       CASE WHEN is_active = true THEN 'Migration — was active' ELSE 'Migration — was inactive' END,
       created_by,
       NOW()
FROM railway_main.coach_types;

-- Quotas
INSERT INTO railway_main.quota_periods (quota_id, effective_from, effective_till, reason, created_by, created_at)
SELECT quota_id,
       COALESCE(created_at::date, CURRENT_DATE),
       CASE WHEN is_active = true THEN NULL ELSE (CURRENT_DATE - INTERVAL '1 day')::date END,
       CASE WHEN is_active = true THEN 'Migration — was active' ELSE 'Migration — was inactive' END,
       created_by,
       NOW()
FROM railway_main.quotas;

-- ── 3. Drop is_active columns ──────────────────────────────────────────────

ALTER TABLE railway_main.trains         DROP COLUMN IF EXISTS is_active;
ALTER TABLE railway_main.train_types    DROP COLUMN IF EXISTS is_active;
ALTER TABLE railway_main.coach_types    DROP COLUMN IF EXISTS is_active;
ALTER TABLE railway_main.quotas         DROP COLUMN IF EXISTS is_active;
ALTER TABLE railway_main.train_coaches  DROP COLUMN IF EXISTS is_active;
ALTER TABLE railway_main.fare_rules     DROP COLUMN IF EXISTS is_active;
ALTER TABLE railway_main.train_schedules DROP COLUMN IF EXISTS is_active;

-- ── 4. Drop the old unique constraint on train_coaches if it references is_active
-- The train_coaches table may have had a unique constraint on (train_id, coach_type_id)
-- which we keep — coach versioning is handled by effective_from/effective_to dates

-- ── Done ───────────────────────────────────────────────────────────────────
