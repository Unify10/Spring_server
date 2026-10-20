CREATE DATABASE onyak;

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 공통 트리거 함수 (updated_at 자동갱신)
CREATE OR REPLACE FUNCTION update_modified_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
RETURN NEW;
END;
$$ language 'plpgsql';

-- ============================================================
-- users (보호자)
-- ============================================================
CREATE TABLE users (
                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255),
                       google_id VARCHAR(255) UNIQUE,
                       name VARCHAR(100) NOT NULL,
                       phone VARCHAR(20) NOT NULL,
                       email_verified BOOLEAN NOT NULL DEFAULT false,
                       birth_date DATE,
                       weight REAL,
                       height REAL,
                       gender VARCHAR(1) CHECK (gender IN ('M', 'F')),
                       is_pregnant BOOLEAN,
                       medication_push_enabled BOOLEAN NOT NULL DEFAULT true,
                       sos_push_enabled BOOLEAN NOT NULL DEFAULT true,
                       marketing_push_enabled BOOLEAN NOT NULL DEFAULT true,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       CHECK (password_hash IS NOT NULL OR google_id IS NOT NULL)
);

CREATE TRIGGER update_users_modtime
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();

-- ============================================================
-- wards (피보호자)
-- ============================================================
CREATE TABLE wards (
                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       guardian_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       name VARCHAR(100) NOT NULL,
                       birth_date DATE NOT NULL,
                       weight REAL NOT NULL,
                       height REAL NOT NULL,
                       gender VARCHAR(1) NOT NULL CHECK (gender IN ('M', 'F')),
                       is_pregnant BOOLEAN NOT NULL DEFAULT false,
                       medication_push_enabled BOOLEAN NOT NULL DEFAULT true,
                       sos_push_enabled BOOLEAN NOT NULL DEFAULT true,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_wards_guardian_id ON wards(guardian_id);

CREATE TRIGGER update_wards_modtime
    BEFORE UPDATE ON wards
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();

-- ============================================================
-- link_codes (연동코드)
-- ============================================================
CREATE TABLE link_codes (
                            id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            ward_id BIGINT NOT NULL REFERENCES wards(id) ON DELETE CASCADE,
                            code VARCHAR(6) NOT NULL,
                            qr_token VARCHAR(255) NOT NULL,
                            attempt_count INT NOT NULL DEFAULT 0,
                            status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','USED','EXPIRED')),
                            expires_at TIMESTAMPTZ NOT NULL,
                            used_at TIMESTAMPTZ,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_link_codes_ward_id ON link_codes(ward_id);

-- ============================================================
-- drugs (의약품 마스터) — 누락됐던 테이블
-- ============================================================
CREATE TABLE drugs (
                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       item_seq VARCHAR(20) NOT NULL UNIQUE,
                       name VARCHAR(255) NOT NULL,
                       ingredients TEXT[] NOT NULL DEFAULT '{}',
                       manufacturer VARCHAR(255),
                       efficacy TEXT,
                       usage_info TEXT,
                       precautions TEXT,
                       barcode VARCHAR(50) UNIQUE,
                       image_url TEXT,
                       efficacy_codes TEXT[] NOT NULL DEFAULT '{}',
                       age_restriction TEXT,
                       pregnancy_restriction TEXT,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_drugs_name_trgm ON drugs USING gin (name gin_trgm_ops);
CREATE INDEX idx_drugs_efficacy_codes ON drugs USING gin (efficacy_codes);

CREATE TRIGGER update_drugs_modtime
    BEFORE UPDATE ON drugs
    FOR EACH ROW EXECUTE PROCEDURE update_modified_column();

-- ============================================================
-- drug_interactions (병용금기 페어)
-- ============================================================
CREATE TABLE drug_interactions (
                                   id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   drug_a_id BIGINT NOT NULL REFERENCES drugs(id),
                                   drug_b_id BIGINT NOT NULL REFERENCES drugs(id),
                                   reason TEXT NOT NULL,
                                   CONSTRAINT uk_drug_interaction_pair UNIQUE (drug_a_id, drug_b_id),
                                   CONSTRAINT chk_drug_pair_order CHECK (drug_a_id < drug_b_id)
);
CREATE INDEX idx_interactions_a ON drug_interactions(drug_a_id);
CREATE INDEX idx_interactions_b ON drug_interactions(drug_b_id);

-- ============================================================
-- medications (가상약장)
-- ============================================================
CREATE TABLE medications (
                             id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             ward_id BIGINT NOT NULL REFERENCES wards(id) ON DELETE CASCADE,
                             drug_id BIGINT NOT NULL REFERENCES drugs(id),
                             dosage VARCHAR(100) NOT NULL,
                             start_date DATE NOT NULL,
                             end_date DATE,
                             schedule_times TIME[] NOT NULL DEFAULT '{}',
                             push_enabled BOOLEAN NOT NULL DEFAULT true,
                             active BOOLEAN NOT NULL DEFAULT true,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_medications_ward_id ON medications(ward_id);
-- 같은 피보호자에게 같은 약이 '복용 중'으로 두 번 들어가지 않게 한다 (중단 이력은 여러 건 허용)
CREATE UNIQUE INDEX uq_medications_ward_drug_active ON medications(ward_id, drug_id) WHERE active;

-- ============================================================
-- medication_checks (복약체크기록)
-- ============================================================
CREATE TABLE medication_checks (
                                   id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   medication_id BIGINT NOT NULL REFERENCES medications(id) ON DELETE CASCADE,
                                   schedule_time TIME NOT NULL,
                                   checked_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_checks_medication_id ON medication_checks(medication_id);
CREATE INDEX idx_checks_checked_at ON medication_checks(checked_at);

-- ============================================================
-- analysis_results (분석결과 스냅샷)
-- ============================================================
CREATE TABLE analysis_results (
                                  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                  medication_id BIGINT NOT NULL REFERENCES medications(id) ON DELETE CASCADE,
                                  has_warning BOOLEAN NOT NULL DEFAULT false,
                                  detail JSONB NOT NULL,
                                  generated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_analysis_medication_id ON analysis_results(medication_id);

-- ============================================================
-- notifications (알림/SOS 히스토리)
-- ============================================================
CREATE TABLE notifications (
                               id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               recipient_type VARCHAR(10) NOT NULL CHECK (recipient_type IN ('GUARDIAN','WARD')),
                               recipient_id BIGINT NOT NULL,
                               type VARCHAR(20) NOT NULL CHECK (type IN ('MED_REMINDER','SOS','WARNING')),
                               message TEXT NOT NULL,
                               location_lat DOUBLE PRECISION,
                               location_lng DOUBLE PRECISION,
                               is_read BOOLEAN NOT NULL DEFAULT false,
                               created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_type, recipient_id, created_at DESC);