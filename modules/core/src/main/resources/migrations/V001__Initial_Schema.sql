-- there are three kinds of users
CREATE TYPE user_type AS ENUM ('standard', 'premium', 'admin');

CREATE TABLE if NOT EXISTS users (
  user_id                UUID NOT NULL PRIMARY KEY,
  google_user_id         NUMERIC UNIQUE NOT NULL,
  email                  VARCHAR UNIQUE NOT NULL
  );
--  user_enabled           BOOLEAN NOT NULL DEFAULT true,
--  user_type              user_type NOT NULL,
--  user_name              VARCHAR NOT NULL
