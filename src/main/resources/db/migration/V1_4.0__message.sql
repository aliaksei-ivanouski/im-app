DROP TABLE IF EXISTS "message";

CREATE TABLE "message"
(
    "id"             UUID    NOT NULL PRIMARY KEY,
    "user_id"        UUID    NOT NULL,
    "chat_id"        UUID    NOT NULL,
    "message_chat_n" INTEGER NOT NULL,
    "version"        INTEGER NOT NULL,
    "payload"        VARCHAR NOT NULL,
    CONSTRAINT message_ux_chat_seq UNIQUE ("chat_id", "message_chat_n")
);

CREATE UNIQUE INDEX "message_ux1"
    ON "message" ("chat_id" ASC, "message_chat_n" DESC, "version" DESC);

CREATE INDEX message_ix_chat_seq
    ON "message" ("chat_id", "message_chat_n" ASC);
