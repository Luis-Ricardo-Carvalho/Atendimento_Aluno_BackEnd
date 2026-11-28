-- Execute once in the Supabase SQL editor before starting the backend.
-- Keep this schema OUT of the Supabase Data API exposed schemas.
CREATE SCHEMA IF NOT EXISTS app_private;
REVOKE ALL ON SCHEMA app_private FROM PUBLIC;
REVOKE ALL ON SCHEMA app_private FROM anon, authenticated;
