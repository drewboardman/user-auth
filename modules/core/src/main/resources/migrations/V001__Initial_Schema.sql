-- there are three kinds of users
CREATE TYPE user_type AS ENUM ('guest', 'standard', 'service');

-- every user has one row here
CREATE TABLE if NOT EXISTS users (
  user_id                user_id NOT NULL PRIMARY KEY
  user_enabled           BOOLEAN NOT NULL DEFAULT true,
  user_type              user_type NOT NULL,
  email                  VARCHAR NOT NULL,
  user_name              VARCHAR NOT NULL
  );