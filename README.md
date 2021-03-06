TODO
----
* need to add encrypted client identifier and decryption
* add redis for sessions
* docker-compose for redis and psql
* github actions
* add jwt auth endpoint that just passes to the google authenticator

How to handle new users
-----------------------
* If it's a current user
  - grab from the DB and return all the user stuff
* If it's a new user
  - just return user created type and redirect them to the user login stuff
