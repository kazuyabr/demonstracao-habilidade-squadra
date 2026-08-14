-- =============================================================================
-- Enterprise Order Processing Platform - Database Initialization
-- Creates the application databases used by the microservices.
-- =============================================================================

IF DB_ID(N'order_db') IS NULL
BEGIN
    CREATE DATABASE order_db;
END;
GO

IF DB_ID(N'payment_db') IS NULL
BEGIN
    CREATE DATABASE payment_db;
END;
GO

IF DB_ID(N'saga_db') IS NULL
BEGIN
    CREATE DATABASE saga_db;
END;
GO
