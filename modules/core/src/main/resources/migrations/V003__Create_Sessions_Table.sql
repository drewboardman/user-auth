CREATE TABLE if NOT EXISTS sessions (
refresh_token          UUID NOT NULL PRIMARY KEY,
user_id                UUID UNIQUE NOT NULL
);
