
CREATE TABLE IF NOT EXISTS sessions (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  status VARCHAR(16) NOT NULL DEFAULT 'OPEN'
);
CREATE TABLE IF NOT EXISTS session_items (
  id UUID PRIMARY KEY,
  session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
  code VARCHAR(128) NOT NULL,
  barcode VARCHAR(128),
  sku VARCHAR(128),
  location VARCHAR(64),
  company VARCHAR(64),
  quantity INTEGER NOT NULL,
  idempotency_key VARCHAR(256),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_items_unique
  ON session_items(session_id, code, COALESCE(company,'0'), COALESCE(location,'0'));
