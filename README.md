# im backend

Reactive IM backend with device-bound authentication, JWT-based API access, and WebSocket messaging. It uses Postgres for persistence and Redis for OTP/device state.

Required flow before messaging:
- Generate a device key pair and keep the private key on the client device.
- Call `POST /auth/start` with `phone` and `deviceId` to receive an OTP and `challenge`.
- Sign the `challenge` bytes using the device private key and send them to `POST /auth/device/verify` with the OTP and public key metadata.
- Store the returned access token and refresh token.
- Create a chat using `POST /chat` with the access token to obtain a `chatId`.
- Connect to `ws://.../ws/messages` using the access token and include `chatId` and `userId` in each WebSocket request payload (for this version one user in the system is enough)

Constraints:
- Authentication is device-bound and requires OTP + signature proof-of-possession.
- You must call `/auth/start` and `/auth/device/verify` to obtain an access token.
- You must create a chat with `POST /chat` before sending messages.
- WebSocket messaging is only allowed after you have a valid access token and a chatId.

## Requirements

- JDK 25 for JVM mode
- GraalVM 25 for native builds
- Docker (optional, for local Postgres/Redis and for building native image in Docker)

## Run locally (Docker Compose)

1) Provide the `.env` file (see `.env.example` for the full list of environment variables).

2) Start the stack:
```bash
docker compose up --build
```

HTTP base URL: `http://localhost:18080`  
WebSocket endpoint: `ws://localhost:18080/ws/messages`

## Build and run native (GraalVM)

Local native build and run:
```bash
./mvnw -Pnative -DskipTests native:compile
./target/im
```

Docker native build and run (uses `Dockerfile`):
```bash
docker build -t im-native .
docker run --env-file .env -p 8080:8080 im-native
```

## Endpoints

### POST /auth/start

Start auth (registers user if missing) and returns OTP + challenge.

Request:
```bash
curl -sS -X POST http://localhost:8080/auth/start \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+12025550123",
    "deviceId": "11111111-1111-1111-1111-111111111111"
  }'
```

Response:
```json
{
  "status": "otp_required",
  "challenge": "B4qO8u9...",
  "challengeExpiresAt": "2026-01-06T10:40:00Z",
  "otpExpiresAt": "2026-01-06T10:40:00Z"
}
```

### POST /auth/device/verify

Verify OTP and prove device key possession by signing the `challenge`.

Request:
```bash
curl -sS -X POST http://localhost:8080/auth/device/verify \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+12025550123",
    "deviceId": "11111111-1111-1111-1111-111111111111",
    "otp": "123456",
    "devicePublicKey": "<base64-x509-public-key>",
    "publicKeyAlg": "EC",
    "signature": "<base64-signature>"
  }'
```

Response:
```json
{
  "accessToken": "eyJ...",
  "accessExpiresInSeconds": 900,
  "refreshToken": "8tQ...",
  "refreshExpiresInSeconds": 2592000
}
```

### POST /auth/refresh

Rotate tokens using the refresh token.

Request:
```bash
curl -sS -X POST http://localhost:8080/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "deviceId": "11111111-1111-1111-1111-111111111111",
    "refreshToken": "8tQ..."
  }'
```

Response:
```json
{
  "accessToken": "eyJ...",
  "accessExpiresInSeconds": 900,
  "refreshToken": "newRefresh...",
  "refreshExpiresInSeconds": 2592000
}
```

### POST /chat

Create a chat (requires access token).

Request:
```bash
curl -sS -X POST http://localhost:8080/chat \
  -H "Authorization: Bearer <accessToken>"
```

Response:
```json
{
  "chatId": "33333333-3333-3333-3333-333333333333"
}
```

### WebSocket /ws/messages

Authenticated WebSocket (send `Authorization: Bearer <accessToken>` during handshake).

Example using `websocat`:
```bash
websocat -H "Authorization: Bearer <accessToken>" ws://localhost:8080/ws/messages
```

#### create_message

Request:
```json
{
  "type": "create_message",
  "requestId": "req-1",
  "payload": {
    "userId": "11111111-1111-1111-1111-111111111111",
    "chatId": "33333333-3333-3333-3333-333333333333",
    "payload": "hello"
  }
}
```

Response:
```json
{
  "type": "create_message_result",
  "requestId": "req-1",
  "status": "ok",
  "payload": {
    "id": "22222222-2222-2222-2222-222222222222",
    "userId": "11111111-1111-1111-1111-111111111111",
    "chatId": "33333333-3333-3333-3333-333333333333",
    "messageChatN": 1,
    "version": 0,
    "payload": "hello"
  },
  "error": null
}
```

#### edit_message

Request:
```json
{
  "type": "edit_message",
  "requestId": "req-2",
  "payload": {
    "messageId": "22222222-2222-2222-2222-222222222222",
    "userId": "11111111-1111-1111-1111-111111111111",
    "chatId": "33333333-3333-3333-3333-333333333333",
    "version": 0,
    "payload": "edited"
  }
}
```

#### list_messages

Request:
```json
{
  "type": "list_messages",
  "requestId": "req-3",
  "payload": {
    "chatId": "33333333-3333-3333-3333-333333333333",
    "userId": "11111111-1111-1111-1111-111111111111",
    "page": 0,
    "size": 20
  }
}
```

Response:
```json
{
  "type": "list_messages_result",
  "requestId": "req-3",
  "status": "ok",
  "payload": {
    "items": [
      {
        "id": "22222222-2222-2222-2222-222222222222",
        "userId": "11111111-1111-1111-1111-111111111111",
        "chatId": "33333333-3333-3333-3333-333333333333",
        "messageChatN": 1,
        "version": 0,
        "payload": "hello"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "error": null
}
```

## Device keys and signing

The server expects:
- `publicKeyAlg`: `EC` or `RSA`
- `devicePublicKey`: base64-encoded X.509 public key (no PEM headers)
- `signature`: base64 signature over the raw UTF-8 bytes of the `challenge` string

### Built-in CLI

Generate a key pair and deviceId:
```bash
./mvnw -q -DskipTests compile exec:java \
  -Dexec.mainClass=com.aivanouski.im.identity.presentation.cli.DeviceSignatureCli \
  -Dexec.args="--generate --alg EC"
```

Sign the `challenge` (use `privateKeyPkcs8` from the previous step):
```bash
./mvnw -q -DskipTests compile exec:java \
  -Dexec.mainClass=com.aivanouski.im.identity.presentation.cli.DeviceSignatureCli \
  -Dexec.args="--sign <challenge> --private-key <base64> --alg EC"
```

### OpenSSL alternative (EC)

```bash
openssl ecparam -name prime256v1 -genkey -noout -out device.key
openssl ec -in device.key -pubout -out device.pub
```

```bash
awk 'NF {sub(/-----.*-----/, ""); print}' device.pub | tr -d '\n'
```

```bash
printf "%s" "<challenge>" > challenge.bin
openssl dgst -sha256 -sign device.key -out signature.bin challenge.bin
base64 < signature.bin
```

## Configuration

Use `.env` or environment variables (see `.env.example`). Common settings:
```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:15432/im_db"
export SPRING_DATASOURCE_USERNAME="imuser"
export SPRING_DATASOURCE_PASSWORD="letmein"
export REDIS_HOST="localhost"
export REDIS_PORT="16379"
export JWT_SECRET="your-strong-secret"
export PHONE_HASH_KEY="<base64-32-bytes>"
export PHONE_ENC_KEY="<base64-32-bytes>"
```

## Notes

- `devicePublicKey` is X.509 base64 (no PEM headers).
- `publicKeyAlg` supports `EC` or `RSA`.
- Refresh tokens are rotated on every refresh; old tokens are revoked.
- Phone numbers are validated and normalized to E.164.
- Phone numbers are stored encrypted with a separate HMAC hash for lookup/uniqueness.
- Once you have tokens, authenticated endpoints do not require phone input.
