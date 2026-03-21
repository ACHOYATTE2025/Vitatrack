CREATE TYPE role_type AS ENUM ('ADMIN', 'USER');


CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    libele role_type UNIQUE NOT NULL
);