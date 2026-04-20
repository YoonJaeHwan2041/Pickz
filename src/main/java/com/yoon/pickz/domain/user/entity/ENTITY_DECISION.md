# User Domain Entity Decision

작성일: 2026-04-20

## 도메인 책임

- 사용자 계정의 인증/인가 기준 정보를 관리한다.
- 판매자 확장 정보(`seller_profiles`, `business_profiles`)와의 경계를 명확히 유지한다.
- 일반 유저/업체 유저/관리자 계정 타입을 단일 사용자 루트에서 분기한다.

## 엔티티 구성

### 1) `users`

- `id`: 사용자 식별자(PK)
- `email`: 로그인 식별 이메일(UNIQUE)
- `password_hash`: 비밀번호 해시
- `nickname`: 서비스 노출 닉네임(UNIQUE)
- `user_type`: 계정 유형(GENERAL, BUSINESS, ADMIN)
- `status`: 계정 상태(ACTIVE, BLOCKED, DELETED)
- `profile_image_url`: 사용자 프로필 이미지 URL
- `created_at`: 생성 시각
- `updated_at`: 수정 시각

### 2) `seller_profiles`

- `id`: 판매자 프로필 식별자(PK)
- `user_id`: 사용자와 1:1 연결되는 키(UNIQUE)
- `seller_display_name`: 판매자 노출명
- `seller_status`: 판매 가능 상태(PENDING, ACTIVE, SUSPENDED)
- `created_at`: 생성 시각
- `updated_at`: 수정 시각

### 3) `business_profiles`

- `id`: 업체 프로필 식별자(PK)
- `seller_profile_id`: 판매자 프로필과 1:1 연결되는 키(UNIQUE)
- `company_name`: 업체명
- `business_number`: 사업자등록번호(UNIQUE)
- `owner_name`: 대표자명
- `business_image_url`: 업체 로고/대표 이미지 URL
- `created_at`: 생성 시각
- `updated_at`: 수정 시각

## 왜 이렇게 분리했는가

- 로그인은 `users` 단일 조회로 끝내고, 판매/업체 정보는 필요 시점에만 확장 조회하기 위해 분리했다.
- 일반 유저가 판매자로 전환되는 시나리오를 단순하게 처리하기 위해 `users`와 `seller_profiles`를 분리했다.
- 업체만 필요한 속성(업체명, 사업자번호)을 `business_profiles`로 격리해 공통 계정 모델 비대화를 막았다.

## 제약/인덱스 정책

- 현재 단계는 UNIQUE만 최소 적용한다.
  - `users.email`
  - `users.nickname`
  - `seller_profiles.user_id`
  - `business_profiles.seller_profile_id`
  - `business_profiles.business_number`
- 일반 인덱스는 초기 단계에서 제외하고, 모니터링 결과로 병목 지점을 확인한 뒤 점진적으로 추가한다.

## 동시성/캐시 관련 메모

- 이 도메인은 입찰 경합의 핵심 구간은 아니므로 낙관적 락 주 대상에서 제외한다.
- Redis는 인증 정본 저장소가 아니라 보조 용도로만 사용하며, 계정 정본은 DB를 기준으로 유지한다.
