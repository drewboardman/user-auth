Design
------
Users are represented by their googleUserId and email. This is sent by the
google auth client library when a user signs in via their google account.

This application creates and logs in users with this information, after their
google jwt is verified by the server-side google oauth library.

When a user is created, we add their googleUserId and email to the users table,
and create our own UUID that is the primary key on the users table.

When a user logs in, we check that they exist in the db - and their session
information. We are using JWTs for sessions, signing them, and applying that
token to the headers of the login response. All authed routes will/should verify
this session information via the JWT.

Some notes about JWTs and handling sessions
-------------------------------------------
* JWTs are signed by the server, so that their validity is ensured
* They have three parts, encoded and `.` delimited
  1. Header - contains encryption alg used to sign
  2. Payload/Claim - any data you want to transfer
  3. Signature
* JWTs can be stolen/intercepted, which devalues this signature as the only
  method of verification
  - you can partially solve this by keeping exiry low (~15 minutes)
  - also don't store JWTs on the client-side (use refresh tokens)

TODO
----
* add refresh token stuff
* add extra user table columns?
* need to add encrypted client identifier and decryption
  - this is used to verify google oauth tokens
  - i don't think it can be encrypted on the client though
* sessions
  - use a jwt and just exchange it with every authed request
* github actions
* allow users to create usernames that are distinct from their google email
* implement google security alerts: https://developers.google.com/identity/protocols/risc
* create a client and serve it
* use the google picture URL
