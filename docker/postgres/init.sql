-- Database-per-service pattern: each microservice owns its own database.
-- No service is allowed to read another service's tables; they talk via APIs or events.
-- authdb is created by POSTGRES_DB; create the others here.
CREATE DATABASE productdb;
CREATE DATABASE orderdb;
