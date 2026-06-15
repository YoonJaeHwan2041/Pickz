# Auth Domain Entity Decision

작성일: 2026-04-20

## 도메인 책임

- JWT 재발급을 위한 리프레시 토큰을 관리한다.
- 토큰 무효화(로그아웃/강제 만료) 이력을 추적한다.

## 엔티티 구성

### 1) `refresh_tokens`

- `id`: 토큰 식별자(PK)
- `user_id`: 사용자 식별자
- `token_hash`: 토큰 해시값(UNIQUE)
- `expires_at`: 만료 시각
- `revoked_at`: 무효화 시각(NULL 가능)
- `created_at`: 생성 시각

## 왜 이렇게 설계했는가

- 원문 토큰 저장 대신 해시 저장으로 유출 위험을 낮춘다.
- 사용자 계정과 인증 이력을 분리해 인증 도메인의 책임을 명확히 한다.
- 만료/무효화 시각을 분리해 보안 정책 집행과 운영 추적을 단순화한다.

## 제약/인덱스 정책

- 현재 단계는 UNIQUE만 최소 적용한다.
  - `refresh_tokens.token_hash`
- 일반 인덱스는 초기 단계에서 제외한다.

## 타임스탬프 정책

- `refresh_tokens`는 로그형 테이블로 분류한다.
- 한번 발급 후 수정이 없는 불변 이력이므로 `created_at`만 유지한다.
- 상태 관리는 `expires_at`(자연 만료), `revoked_at`(강제 무효화)으로 처리한다.
- `updated_at`, `deleted_at`은 의도적으로 제외한다.
- BaseEntity 적용 대상에서 제외하고 별도로 관리한다.

## Redis 메모

- Redis를 인증 보조 캐시로 사용할 수 있으나, 토큰 정본과 폐기 이력은 DB 기준으로 관리한다.

---

## 구현 결정사항 (2026-06-15 추가)

### JWT 라이브러리 선택 — jjwt

- **선택**: `io.jsonwebtoken:jjwt`
- **이유**: Spring Boot 생태계에서 가장 널리 쓰이는 JWT 라이브러리. 서명 알고리즘(HMAC-SHA256) 지원, 빌더 패턴으로 가독성이 높고 검증 로직이 단순함

### Access Token / Refresh Token 키 분리

- Access Token과 Refresh Token에 **서로 다른 시크릿 키**를 사용한다 (`jwt.secret`, `jwt.refresh-key`)
- **이유**: 키가 하나면 Refresh Token으로 Access Token을 위조하거나 반대로 악용하는 공격이 가능함. 키를 분리하면 용도별 토큰의 경계가 명확해지고 한쪽 키가 유출돼도 다른 토큰은 안전

### Refresh Token 해싱 — SHA-256

- DB에 Refresh Token 원문 대신 **SHA-256 해시값**을 저장한다
- **이유**: DB가 탈취되더라도 원문 토큰을 복원할 수 없어 피해 최소화. 검증 시에는 요청으로 받은 토큰을 똑같이 해싱해서 비교하므로 기능 손실 없음

### 로그인 처리 흐름

```
1. 이메일 형식 검증 (Pattern 매칭)
2. DB에서 이메일로 User 조회
3. UserStatus.ACTIVE 여부 확인 (탈퇴/정지 계정 차단)
4. BCrypt 비밀번호 검증
5. Access Token 발급 (userId, email 클레임 포함)
6. Refresh Token 발급 + SHA-256 해싱 후 DB 저장
7. 응답: Access Token → body, Refresh Token → HttpOnly Cookie
```

- 2~4번이 실패하면 **어느 단계에서 실패했는지 노출하지 않고** `INVALID_CREDENTIALS` 단일 에러로 응답 (이메일 존재 여부 노출 방지)
