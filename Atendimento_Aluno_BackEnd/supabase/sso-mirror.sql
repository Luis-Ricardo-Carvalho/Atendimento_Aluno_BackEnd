-- Execute once before deploying the SSO version. Existing UUIDs and FKs are preserved.
-- No Flyway/Liquibase migrations exist in this repository.
BEGIN;
ALTER TABLE app_private.usuarios ALTER COLUMN email DROP NOT NULL;
ALTER TABLE app_private.usuarios ALTER COLUMN matricula DROP NOT NULL;
ALTER TABLE app_private.usuarios ALTER COLUMN senha DROP NOT NULL;
UPDATE app_private.usuarios SET senha = NULL;
COMMIT;
