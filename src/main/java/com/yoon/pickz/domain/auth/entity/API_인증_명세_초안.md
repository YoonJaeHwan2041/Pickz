# Pickz 인증 API 명세 (초안)

작성일: 2026-04-25

## 문서 목적

- 인증 도메인(Auth)의 API 계약을 우선 정의한다.
- 이후 경매/입찰 API에서 공통 인증 규칙을 동일하게 적용한다.

---

## 공통 규칙

- Base URL: `/api/v1`
- 시간 포맷: UTC (ISO-8601)
- 공통 응답 포맷: `success`, `data`, `error`, `timestamp`
- 인증 정책:
  - Access Token: 응답 body(JSON)로 전달
  - Refresh Token: HttpOnly Cookie로 전달

---

## 1) 회원가입

### `POST /auth/signup`

### 목적

- 신규 계정을 생성한다.

### 인증

- 불필요

### Request Body

```json
{
  "email": "user@example.com",
  "password": "P@ssw0rd!",
  "nickname": "pickzer",
  "userType": "GENERAL"
}
```

- `userType`: `GENERAL` 또는 `BUSINESS` (가입 시 선택)

### Success Response (201)

```json
{
  "success": true,
  "data": {
    "userId": 1001,
    "email": "user@example.com",
    "userType": "GENERAL"
  },
  "error": null,
  "timestamp": "2026-04-25T10:15:30Z"
}
```

### Error Response

- `409 CONFLICT` / `EMAIL_ALREADY_EXISTS` / `이미 있는 이메일입니다.`
- `409 CONFLICT` / `NICKNAME_ALREADY_EXISTS` / `이미 있는 닉네임입니다.`
- `400 BAD_REQUEST` / `INVALID_REQUEST` / 유효성 검증 실패

---

## 2) 로그인

### `POST /auth/login`

### 목적

- 이메일/비밀번호 인증 후 토큰을 발급한다.

### 인증

- 불필요

### Request Body

```json
{
  "email": "user@example.com",
  "password": "P@ssw0rd!"
}
```

### Success Response (200)

Response Body:

```json
{
  "success": true,
  "data": {
    "accessToken": "<jwt-access-token>",
    "accessTokenExpiresAt": "2026-04-25T11:15:30Z"
  },
  "error": null,
  "timestamp": "2026-04-25T10:15:30Z"
}
```

Set-Cookie (Refresh Token):

```http
Set-Cookie: refreshToken=<jwt-refresh-token>; HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth; Max-Age=1209600
```

### Error Response

- `400 BAD_REQUEST` / `INVALID_EMAIL_FORMAT` / `이메일 형식으로 로그인해 주세요.`
- `401 UNAUTHORIZED` / `INVALID_CREDENTIALS` / `이메일 또는 비밀번호가 올바르지 않습니다.`
- `400 BAD_REQUEST` / `INVALID_REQUEST` / 유효성 검증 실패

---

## 3) 토큰 재발급

### `POST /auth/refresh`

### 목적

- Refresh Token으로 Access Token을 재발급한다.

### 인증

- Access Token 불필요
- HttpOnly Cookie의 `refreshToken` 필요

### Request

- Body 없음
- Cookie 포함

### Success Response (200)

```json
{
  "success": true,
  "data": {
    "accessToken": "<new-jwt-access-token>",
    "accessTokenExpiresAt": "2026-04-25T12:15:30Z"
  },
  "error": null,
  "timestamp": "2026-04-25T11:15:30Z"
}
```

### Error Response

- `401 UNAUTHORIZED` / `REFRESH_TOKEN_INVALID` / `유효하지 않은 리프레시 토큰입니다.`
- `401 UNAUTHORIZED` / `REFRESH_TOKEN_EXPIRED` / `만료된 리프레시 토큰입니다.`

---

## 4) 로그아웃

### `POST /auth/logout`

### 목적

- 현재 세션의 Refresh Token을 무효화한다.

### 인증

- Access Token 필요(권장)
- Cookie의 `refreshToken` 필요

### Request

- Body 없음

### Success Response (200)

```json
{
  "success": true,
  "data": {
    "message": "로그아웃이 완료되었습니다."
  },
  "error": null,
  "timestamp": "2026-04-25T11:20:00Z"
}
```

### 쿠키 만료 처리

```http
Set-Cookie: refreshToken=; HttpOnly; Secure; SameSite=Lax; Path=/api/v1/auth; Max-Age=0
```

### Error Response

- `401 UNAUTHORIZED` / `UNAUTHORIZED` / 인증 실패

---

## 소셜 로그인 확장 계획 (현재 미구현)

- 현재 단계는 이메일 로그인만 구현한다.
- 소셜 로그인은 확장 항목으로 분리한다.
- 후보: Google, Kakao
- 계정 통합 정책(동일 이메일 병합 여부)은 추후 확정한다.
