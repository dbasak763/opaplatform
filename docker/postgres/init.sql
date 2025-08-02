-- Set password for postgres user
ALTER USER postgres PASSWORD 'postgres';

-- Create user if not exists (for external connections)
DO
$do$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE  rolname = 'orderuser') THEN
      CREATE ROLE orderuser LOGIN PASSWORD 'orderpass';
   END IF;
END
$do$;

-- Create additional databases if needed
CREATE DATABASE orderdb_test;

-- Grant privileges to orderuser
GRANT ALL PRIVILEGES ON DATABASE orderdb TO orderuser;
GRANT ALL PRIVILEGES ON DATABASE orderdb_test TO orderuser;

-- Grant schema permissions for Flyway migrations
GRANT CREATE ON SCHEMA public TO orderuser;
GRANT USAGE ON SCHEMA public TO orderuser;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO orderuser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO orderuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO orderuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO orderuser;
ALTER USER orderuser CREATEDB;

-- Create extensions
\c orderdb;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c orderdb_test;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
