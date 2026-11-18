CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE companies (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name   TEXT        NOT NULL,
    bin    VARCHAR(12) NOT NULL UNIQUE,
    phone  VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('NEW', 'NO_ANSWER', 'REFUSED', 'MEETING_SET'))
);

CREATE TABLE managers (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE meetings (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id BIGINT      NOT NULL REFERENCES companies,
    manager_id BIGINT      NOT NULL REFERENCES managers,
    starts_at  TIMESTAMPTZ NOT NULL,
    ends_at    TIMESTAMPTZ NOT NULL,
    status     VARCHAR(16) NOT NULL CHECK (status IN ('SCHEDULED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL,
    -- полуинтервал [starts_at, ends_at): встречи встык не пересекаются
    CONSTRAINT meetings_manager_no_overlap EXCLUDE USING gist (
        manager_id WITH =,
        tstzrange(starts_at, ends_at) WITH &&
    ) WHERE (status = 'SCHEDULED')
);

CREATE INDEX meetings_company_id_idx ON meetings (company_id);

CREATE TABLE meeting_reschedules (
    meeting_id         BIGINT      NOT NULL REFERENCES meetings,
    seq                INT         NOT NULL,
    previous_starts_at TIMESTAMPTZ NOT NULL,
    new_starts_at      TIMESTAMPTZ NOT NULL,
    rescheduled_at     TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (meeting_id, seq)
);

CREATE TABLE calls (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    company_id BIGINT      NOT NULL REFERENCES companies,
    result     VARCHAR(16) NOT NULL CHECK (result IN ('NO_ANSWER', 'REFUSED', 'MEETING_SET')),
    meeting_id BIGINT REFERENCES meetings,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT calls_meeting_only_for_meeting_set CHECK ((result = 'MEETING_SET') = (meeting_id IS NOT NULL))
);
