CREATE TABLE products (
    id       SERIAL PRIMARY KEY,
    sku      VARCHAR(50)     NOT NULL,
    name     VARCHAR(200)    NOT NULL,
    price    NUMERIC(10, 2)  NOT NULL,
    currency VARCHAR(3)      NOT NULL DEFAULT 'USD'
);

CREATE TABLE risk_profiles (
    id        SERIAL PRIMARY KEY,
    client_id VARCHAR(50) NOT NULL,
    score     INTEGER     NOT NULL CHECK (score BETWEEN 0 AND 100),
    category  VARCHAR(10) NOT NULL CHECK (category IN ('LOW', 'MEDIUM', 'HIGH'))
);
