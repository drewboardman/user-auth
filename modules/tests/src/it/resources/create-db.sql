CREATE DATABASE "user-auth";
create user postgres with encrypted password 'postgres';
grant all privileges on database 'user-auth' to postgres;