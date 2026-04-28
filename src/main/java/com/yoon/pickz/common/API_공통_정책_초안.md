# Pickz API 공통 정책 (초안)

작성일: 2026-04-25

## 문서 목적

- API 명세 작성 전에 모든 도메인이 공유할 공통 규칙을 고정한다.
- 프론트엔드/백엔드 간 계약(Contract)을 먼저 정의해 이후 변경 비용을 줄인다.

---

## 1) Base URL / 버전 정책

- Base URL: `/api/v1`
- 버전 prefix를 사용하는 이유
  - 향후 스펙 변경 시 `v2`를 병행 제공해 하위 호환을 유지하기 위함
  - 구버전/신버전의 차이를 명시적으로 관리하기 위함
- 상위 버전 도입 시 문서 규칙
  - 변경 API에 "구형(v1) / 신형(v2)" 차이를 주석으로 반드시 기록한다.
- 버전 폐기 정책
  - `v2` 배포 후 `v1`은 6개월 동안 유지한다.
  - 유지 기간 종료 후 `v1`은 순차적으로 종료한다.

---

## 2) 시간(Time) 정책

- 저장 기준: UTC
- 응답 기준: UTC (ISO-8601)
- 클라이언트(웹/모바일)에서 사용자 타임존(KST)으로 변환해 표시

예시:

```json
"createdAt": "2026-04-25T10:15:30Z"
```

---

## 3) 금액(Money) 정책

- 금액 타입: `DECIMAL`
- 컬럼 정밀도: `DECIMAL(18,2)`
- 이유
  - 소수 단위 확장 가능성을 열어 둔다.
  - 부동소수(float/double) 오차 없이 정밀한 금액 처리가 가능하다.

---

## 4) 인증/인가 정책

- 인증 방식: JWT Bearer Token
- Access Token 전달 방식: 응답 body(JSON) + 요청 헤더 Bearer
- Refresh Token 전달 방식: HttpOnly Cookie
- 요청 헤더:

```http
Authorization: Bearer <accessToken>
```

- Access Token 만료 시 Refresh Token 재발급 흐름 사용
- 보호 자원 호출 시 인증 실패는 `401 Unauthorized`로 처리

### Refresh Token Cookie 옵션

- `HttpOnly=true`: JavaScript 접근 차단(XSS 위험 완화)
- `Secure=true`: HTTPS에서만 전송(개발 로컬 HTTP 환경은 예외 처리 가능)
- `SameSite=Lax`: 일반적인 CSRF 위험을 낮추면서 기본 사용성을 유지
- `Path=/api/v1/auth`: 인증 API 경로에만 쿠키를 전송
- `Max-Age=1209600`: 14일(초 단위), 만료 시 자동 폐기

---

## 5) 공통 응답 포맷

- 공통 응답 body를 사용한다.
- 성공/실패를 동일 구조로 반환해 클라이언트 파싱 규칙을 단순화한다.

예시(성공):

```json
{
  "success": true,
  "data": {
    "id": 1001
  },
  "error": null,
  "timestamp": "2026-04-25T10:15:30Z"
}
```

예시(실패):

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "EMAIL_ALREADY_EXISTS",
    "message": "이미 있는 이메일입니다.",
    "details": [
      "email must be unique"
    ],
    "traceId": "cbf3e0a1d2f14d29"
  },
  "timestamp": "2026-04-25T10:15:30Z"
}
```
- 실패 응답 에러 스키마는 아래 필드를 필수로 고정한다.
  - `code`
  - `message`
  - `details`
  - `traceId`

---

## 6) 페이징 공통 포맷

- 목록 조회 API는 아래 공통 페이징 메타를 사용한다.
  - `page`
  - `size`
  - `totalElements`
  - `totalPages`
  - `hasNext`

예시:

```json
{
  "success": true,
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 135,
    "totalPages": 7,
    "hasNext": true
  },
  "error": null,
  "timestamp": "2026-04-25T10:15:30Z"
}
```

---

## 7) HTTP 상태코드 기본 원칙

- `200 OK`: 조회/수정 성공
- `201 Created`: 생성 성공
- `400 Bad Request`: 요청 값/비즈니스 규칙 위반
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 부족
- `404 Not Found`: 리소스 없음
- `409 Conflict`: 중복/충돌(예: 이메일 중복)
- `500 Internal Server Error`: 서버 내부 오류

---

## 8) 에러코드 표준

### 네이밍 규칙

- 형식: `도메인_원인`
- 패턴: `UPPER_SNAKE_CASE`
- 예시
  - `AUTH_INVALID_CREDENTIALS`
  - `USER_EMAIL_ALREADY_EXISTS`
  - `AUCTION_NOT_FOUND`

### 도메인 Prefix

- `COMMON`: 공통 예외(파싱 실패, 내부 오류 등)
- `AUTH`: 인증/인가 도메인
- `USER`: 사용자 도메인
- `AUCTION`: 경매 도메인
- `BID`: 입찰 도메인

### HTTP 상태코드 매핑 원칙

- `400 Bad Request`: 요청 형식/검증 실패
  - 예: `COMMON_INVALID_REQUEST`, `COMMON_VALIDATION_ERROR`
- `401 Unauthorized`: 인증 실패(토큰 없음/만료/서명 오류)
  - 예: `AUTH_UNAUTHORIZED`, `AUTH_ACCESS_TOKEN_EXPIRED`
- `403 Forbidden`: 인증은 되었지만 권한 부족
  - 예: `AUTH_FORBIDDEN`
- `404 Not Found`: 대상 리소스 없음
  - 예: `USER_NOT_FOUND`, `AUCTION_NOT_FOUND`
- `409 Conflict`: 중복/상태 충돌
  - 예: `USER_EMAIL_ALREADY_EXISTS`, `BID_CONFLICT`
- `500 Internal Server Error`: 서버 내부 오류
  - 예: `COMMON_INTERNAL_ERROR`

### 공통 에러코드 최소 목록 (MVP)

- `COMMON_INVALID_REQUEST` (`400`)
- `COMMON_VALIDATION_ERROR` (`400`)
- `COMMON_METHOD_NOT_ALLOWED` (`405`)
- `COMMON_UNSUPPORTED_MEDIA_TYPE` (`415`)
- `COMMON_INTERNAL_ERROR` (`500`)
- `AUTH_UNAUTHORIZED` (`401`)
- `AUTH_FORBIDDEN` (`403`)

---

## 9) 미정 항목 체크리스트

- [x] API 버전 폐기(deprecation) 정책
- [x] 에러 응답 스키마 고정 수준
- [x] 에러코드 네이밍/매핑 표준
- [x] 금액 DECIMAL 정밀도
- [x] 페이징 공통 포맷 (`page`, `size`, `totalElements` 등)
- [ ] 정렬/필터 쿼리 파라미터 표준

