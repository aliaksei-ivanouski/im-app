CREATE TABLE "chat"
(
    "id"         UUID      NOT NULL PRIMARY KEY,
    "created_at" TIMESTAMP NOT NULL DEFAULT NOW()
);
