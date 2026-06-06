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
