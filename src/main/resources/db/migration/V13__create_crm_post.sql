-- CRM posts (server-rendered MVC and SSE change notifications)

CREATE TABLE crm_post (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    body TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crm_post_created_at ON crm_post (created_at DESC);
