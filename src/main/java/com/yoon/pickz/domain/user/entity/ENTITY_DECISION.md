# User Domain Entity Decision

작성일: 2026-04-20

## 도메인 책임

- 사용자 계정의 인증/인가 기준 정보를 관리한다.
- 일반 판매자 프로필(`seller_profiles`)과 사업체 판매 프로필(`business_profiles`)의 경계를 명확히 유지한다.
- 사업체 사용자 운영을 위해 사업체-사용자 매핑(`business_members`)을 관리한다.
- 일반 유저/업체 유저/관리자 계정 타입을 단일 사용자 루트에서 분기한다.

## 엔티티 구성

### 1) `users`

- `id`: 사용자 식별자(PK)
- `email`: 로그인 식별 이메일(UNIQUE)
- `password`: 비밀번호 해시
- `nickname`: 서비스 노출 닉네임(UNIQUE)
- `user_type`: 계정 유형(GENERAL, BUSINESS, ADMIN)
- `status`: 계정 상태(ACTIVE, BLOCKED, DELETED)
- `profile_image_url`: 사용자 프로필 이미지 URL
- `created_at`: 생성 시각
- `updated_at`: 수정 시각
- `deleted_at`: 삭제 시각

### 2) `seller_profiles`

- `id`: 판매자 프로필 식별자(PK)
- `user_id`: 일반 사용자와 연결되는 키(UNIQUE)
- `seller_display_name`: 판매자 노출명
- `seller_status`: 판매 가능 상태(PENDING, ACTIVE, SUSPENDED)
- `created_at`: 생성 시각
- `updated_at`: 수정 시각
- `deleted_at`: 판매자 프로필 소프트 삭제 시각 ← 2026-05-29 추가

### 3) `business_profiles`

- `id`: 업체 프로필 식별자(PK)
- ~~`owner_user_id`: 사업체 대표 계정 사용자 식별자~~ ← 2026-05-29 1차 명확화, 2026-05-29 최종 제거 (아래 변경 이력 참고)
- `company_name`: 업체명
- `business_number`: 사업자등록번호(UNIQUE)
- `owner_name`: 대표자명
- `business_image_url`: 업체 로고/대표 이미지 URL
- `created_at`: 생성 시각
- `updated_at`: 수정 시각
- `deleted_at`: 소프트 삭제 시각 ← 2026-05-29 추가

### 4) `business_members`

- `id`: 사업체 멤버십 식별자(PK)
- `business_profile_id`: 소속 사업체 식별자
- `user_id`: 멤버 사용자 식별자
- `member_role`: 멤버 역할(OWNER, MANAGER, STAFF)
- `member_status`: 멤버 상태(ACTIVE, INVITED, REMOVED)
- `joined_at`: 사업체 합류 시각
- `left_at`: 사업체 탈퇴/역할 해제 시각(NULL 허용) ← 2026-05-29 추가
- `created_at`: 생성 시각
- `updated_at`: 수정 시각
- `deleted_at`: 소프트 삭제 시각 ← 2026-05-29 추가

## 왜 이렇게 분리했는가

- 로그인은 `users` 단일 조회로 끝내고, 판매 정보는 유형별로 분리해 관리한다.
- `seller_profiles`는 일반 사용자 전용 판매 프로필로 사용한다.
- `business_profiles`는 사업체 사용자 전용 판매 프로필로 사용한다.
- 두 프로필은 의미와 운영 정책이 다르므로 직접 FK로 연결하지 않는다.
- 공용 계정을 피하고 사용자별 행위 추적을 가능하게 하기 위해 `business_members`를 사용해 사업체 1:N 사용자 구조를 지원한다.

### `business_members` 단일 관리 정책 (2026-05-29 최종 확정)

- ~~`business_profiles.owner_user_id`로 최초 생성자를 별도 관리한다.~~ → 제거됨
- 모든 대표자 이력은 `business_members` 단일 테이블에서 관리한다.
- `joined_at` + `left_at` + `member_role = OWNER` 조합으로 완전한 승계 이력 추적이 가능하다.

| 조회 목적 | 쿼리 기준 |
|---|---|
| 현재 대표자 | `member_role = OWNER AND member_status = ACTIVE` |
| 과거 대표자 이력 | `member_role = OWNER AND member_status = REMOVED ORDER BY joined_at` |
| 최초 생성자 | `member_role = OWNER ORDER BY joined_at ASC LIMIT 1` |

## Enum 상태 전환 규칙

### `UserStatus` (users.status)

```
ACTIVE ──→ BLOCKED   (관리자 제재)
ACTIVE ──→ DELETED   (회원 탈퇴 — 복구 불가)
BLOCKED ──→ ACTIVE   (관리자 제재 해제)
BLOCKED ──→ DELETED  (탈퇴 처리)
DELETED ──→ (전환 없음, 최종 상태)
```

### `UserType` (users.user_type)

- 가입 시 `GENERAL` 또는 `BUSINESS` 중 선택
- 이후 변경 불가 (변경이 필요하면 재가입)

### `SellerStatus` (seller_profiles.seller_status)

```
PENDING ──→ ACTIVE     (판매자 승인)
ACTIVE ──→ SUSPENDED   (판매 정지)
SUSPENDED ──→ ACTIVE   (정지 해제, 복구 가능)
```

### `MemberRole` (business_members.member_role)

- `OWNER`: 대표자 — 대표자 승계 시 기존 OWNER는 MANAGER로 변경
- `MANAGER`: 관리자
- `STAFF`: 일반 직원
- 역할 변경은 현재 OWNER만 가능

### `MemberStatus` (business_members.member_status)

```
INVITED ──→ ACTIVE    (초대 수락)
INVITED ──→ REMOVED   (초대 거절 또는 취소)
ACTIVE ──→ REMOVED    (탈퇴 또는 강제 제거)
REMOVED ──→ (전환 없음, 재초대 시 새 레코드 생성)
```

> REMOVED → 재참여는 기존 레코드를 변경하지 않고 새 레코드를 INSERT한다. `left_at`이 있는 레코드는 이력으로 유지한다.

## 제약/인덱스 정책

- 현재 단계는 UNIQUE만 최소 적용한다.
  - `users.email`
  - `users.nickname`
  - `seller_profiles.user_id`
  - `business_profiles.business_number`
- `business_members (business_profile_id, user_id)` 조합 UNIQUE
- 일반 인덱스는 초기 단계에서 제외하고, 모니터링 결과로 병목 지점을 확인한 뒤 점진적으로 추가한다.

## 동시성/캐시 관련 메모

- 이 도메인은 입찰 경합의 핵심 구간은 아니므로 낙관적 락 주 대상에서 제외한다.
- Redis는 인증 정본 저장소가 아니라 보조 용도로만 사용하며, 계정 정본은 DB를 기준으로 유지한다.

## 2026-04-20 매핑 테이블 설계 고민과 결론

### 고민 1) 사업체를 단일 계정으로 운영할지, 다중 계정으로 운영할지

- 단일 계정 운영은 구현이 단순하지만, 실무에서 "누가 어떤 작업을 했는지" 추적이 어렵다.
- 다중 계정 운영은 구조가 조금 복잡해지지만, 사용자별 권한과 행위 로그 추적이 가능하다.
- 결론: 운영 추적성과 보안을 위해 다중 계정 운영을 선택했다.

### 고민 2) `business_profiles`에 사용자 FK를 직접 둘지, 별도 매핑 테이블을 둘지

- `business_profiles.user_id` 같은 직접 FK는 1:1에 가깝고, 팀 단위 운영으로 확장하기 어렵다.
- 별도 매핑 테이블(`business_members`)은 1:N 멤버 구조와 역할 분리를 자연스럽게 지원한다.
- 결론: 사업체-사용자 관계는 `business_members`를 기준으로 관리한다.

### 고민 3) `seller_profiles`와 `business_profiles`를 연결할지

- 두 모델은 판매 주체가 다르다.
  - `seller_profiles`: 일반 사용자 개인 판매
  - `business_profiles`: 사업체 단위 판매
- 직접 연결하면 도메인 경계가 흐려지고 정책 충돌이 발생하기 쉽다.
- 결론: 둘은 분리하고, 공통 사용자 루트(`users`)와 매핑(`business_members`)로만 관계를 관리한다.

### 매핑 테이블 도입 효과

- 사업체 내 다계정 운영 가능(OWNER, MANAGER, STAFF)
- 공용 계정 사용을 피하고 사용자별 감사 추적 가능
- 팀원 초대/권한 변경/탈퇴 등 운영 기능 확장 용이

---

## 변경 이력

### 2026-05-29 — 1차: `owner_user_id` 역할 명확화

- **변경 내용**: `business_profiles.owner_user_id`의 역할을 "대표 계정"에서 "최초 생성자 불변 이력"으로 명확화
- **변경 이유**: 문서에 역할이 명시되지 않아 `business_members`의 `OWNER`와 혼선이 발생. 실제 의도는 대표자 승계 히스토리 추적을 위한 불변 이력 용도였음
- **당시 결론**: 현재 대표자는 `business_members`, 최초 생성자는 `owner_user_id`로 역할 분리

### 2026-05-29 — 2차: `owner_user_id` 제거 + `business_members` 단일 관리로 전환

- **변경 내용**:
  - `business_profiles.owner_user_id` 제거
  - `business_members.left_at (DATETIME, NULL)` 추가
  - `seller_profiles.deleted_at (DATETIME, NULL)` 추가
- **변경 이유**:
  - `business_members`에 `left_at`을 추가하면 `joined_at` + `left_at` + `member_role` 조합으로 전체 대표자 이력(최초 생성자 포함)을 단일 테이블에서 완전하게 관리할 수 있음
  - `owner_user_id`는 이 구조에서 중복 정보가 되므로 단일 책임 원칙에 따라 제거
  - `seller_profiles`에 `deleted_at`이 없으면 `SUSPENDED`(일시 중단)와 실제 삭제를 구분할 수 없어 추가
- **최종 정책**:
  - 대표자 이력 정본 → `business_members` 단일 테이블
  - 최초 생성자 → `member_role = OWNER ORDER BY joined_at ASC LIMIT 1`
  - 판매자 프로필 삭제 → `seller_profiles.deleted_at`으로 소프트 삭제 관리
