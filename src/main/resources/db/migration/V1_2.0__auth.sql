CREATE TABLE "app_user"
(
    "id"              UUID        NOT NULL PRIMARY KEY,
    "phone_hash"      VARCHAR     NOT NULL UNIQUE,
    "phone_encrypted" VARCHAR     NOT NULL,
    "created_at"      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE "device"
(
    "id"                    UUID        NOT NULL PRIMARY KEY,
    "user_id"               UUID        NOT NULL REFERENCES "app_user" ("id"),
    "public_key"            VARCHAR,
    "public_key_alg"        VARCHAR,
    "challenge"             VARCHAR,
    "challenge_expires_at"  TIMESTAMP,
    "verified_at"           TIMESTAMP,
    "created_at"            TIMESTAMP   NOT NULL DEFAULT NOW(),
    "last_seen_at"          TIMESTAMP
);

CREATE INDEX device_ix_user
    ON "device" ("user_id");

CREATE TABLE "refresh_token"
(
    "id"            UUID        NOT NULL PRIMARY KEY,
    "user_id"       UUID        NOT NULL REFERENCES "app_user" ("id"),
    "device_id"     UUID        NOT NULL REFERENCES "device" ("id"),
    "token_hash"    VARCHAR     NOT NULL UNIQUE,
    "expires_at"    TIMESTAMP   NOT NULL,
    "revoked_at"    TIMESTAMP,
    "created_at"    TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX refresh_token_ix_user
    ON "refresh_token" ("user_id");
