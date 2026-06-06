# Auction Domain Entity Decision

작성일: 2026-04-20

## 도메인 책임

- 경매 생성/진행/종료 상태를 관리한다.
- 경매 이미지, 경매 결과를 경매 본체와 분리해 유지보수성을 확보한다.
- 자동 연장 정책을 경매 단위로 관리한다.

## 엔티티 구성

### 1) `auctions`

- `id`: 경매 식별자(PK)
- `seller_id`: 판매자 사용자 식별자
- `title`: 경매 제목
- `description`: 상품 설명
- `start_price`: 시작가
- `bid_unit`: 최소 입찰 단위
- `current_price`: 현재 최고 입찰가
- `start_at`: 시작 시각
- `end_at`: 현재 종료 시각(연장 시 변경)
- `max_end_at`: 최대 연장 가능 시각(NULL 허용)
- `extension_window_sec`: 종료 임박 판정 구간(N초)
- `extension_sec`: 1회 연장 시간(M초)
- `status`: 경매 상태(SCHEDULED, LIVE, ENDED, CANCELLED)
- `version`: 낙관적 락 버전
- `created_at`: 생성 시각
- `updated_at`: 수정 시각
- `deleted_at`: 소프트 삭제 시각 ← 2026-05-29 추가

### 2) `auction_images`

- `id`: 이미지 식별자(PK)
- `auction_id`: 대상 경매 식별자
- `image_url`: 이미지 주소
- `image_type`: 이미지 타입(THUMBNAIL, DETAIL)
- `sort_order`: 노출 순서
- `created_at`: 생성 시각
- `updated_at`: 수정 시각 ← 2026-05-29 추가
- `deleted_at`: 소프트 삭제 시각 ← 2026-05-29 추가

### 3) `auction_results`

- `id`: 결과 식별자(PK)
- `auction_id`: 결과 대상 경매 식별자(UNIQUE)
- `winner_id`: 낙찰 사용자 식별자(NULL 가능)
- `winning_bid_id`: 낙찰 입찰 식별자(NULL 가능)
- `final_price`: 최종 낙찰가(NULL 가능)
- `result_status`: 결과 상태(SOLD, UNSOLD, CANCELLED)
- `closed_at`: 종료 확정 시각
- `created_at`: 결과 생성 시각

## 왜 이렇게 분리했는가

- 경매 본체(`auctions`)와 종료 결과(`auction_results`)를 분리해 단일 테이블 과부하를 줄였다.
- 경매 상세 화면에서 결과가 필요하면 조인해 조회하되, 정산/통계/종료 후 처리 로직은 결과 테이블 중심으로 단순화한다.
- 이미지를 별도 테이블로 분리해 다중 이미지 및 타입별 관리 요구를 수용한다.
- `max_end_at`을 NULL 허용으로 둬 상한 없는 연장 정책도 허용한다.

## Enum 상태 전환 규칙

### `AuctionStatus` (auctions.status)

```
SCHEDULED ──→ LIVE       (경매 시작 시각 도달)
SCHEDULED ──→ CANCELLED  (시작 전 취소)
LIVE ──→ ENDED           (종료 시각 도달 + auction_results 생성)
LIVE ──→ CANCELLED       (진행 중 강제 취소)
ENDED ──→ (전환 없음, 최종 상태)
CANCELLED ──→ (전환 없음, 최종 상태)
```

> `ENDED` 전환은 반드시 `auction_results` INSERT와 단일 트랜잭션으로 처리한다.

### `ImageType` (auction_images.image_type)

- `THUMBNAIL`: 경매 목록/상세 대표 이미지 — 경매당 1장
- `DETAIL`: 상품 상세 이미지 — 여러 장 가능, `sort_order`로 순서 관리
- 타입 변경은 허용하지 않는다 (삭제 후 재등록)

### `ResultStatus` (auction_results.result_status)

- 최초 저장 시 확정되며 이후 변경하지 않는다.
- `SOLD`: 낙찰 — `winner_id`, `winning_bid_id`, `final_price` 모두 필수
- `UNSOLD`: 유찰 — 입찰 없이 종료, 세 필드 모두 NULL
- `CANCELLED`: 경매 취소로 인한 결과 — 세 필드 모두 NULL

## 제약/인덱스 정책

- 현재 단계는 UNIQUE만 최소 적용한다.
  - `auction_results.auction_id` (경매당 결과 1건 보장)
- 일반 인덱스는 초기 단계에서 제외한다.

## 동시성 전략 (토론 반영)

- 초기 구현은 낙관적 락(`auctions.version`)으로 시작한다.
- 입찰 충돌은 정상 시나리오로 보고 재시도 또는 실패 응답으로 처리한다.
- 부하/경합 테스트에서 충돌률이 높으면 비관적 락 또는 Redis 분산 락 전환을 검토한다.
- 결론: 초기에는 낙관적으로 빠르게 검증하고, 테스트 결과 기반으로 비관적 전략을 선택한다.

## Redis 역할

- Redis는 실시간성 보조(상태 전달, 캐시) 및 병목 완화를 위한 보조 계층으로 활용한다.
- 경매/결과 정본 데이터는 DB를 기준으로 관리한다.

## 타임스탬프 정책

- 도메인 엔티티와 로그형 테이블을 구분해 타임스탬프 구성을 다르게 가져간다.
- `auctions`, `auction_images`: 상태가 변하고 소프트 삭제가 의미 있는 도메인 엔티티 → `created_at` + `updated_at` + `deleted_at` (BaseEntity 적용)
- `auction_results`: 종료 결과 확정본으로 한번 저장 후 수정/삭제 없는 로그형 → `created_at`만 유지

## 변경 이력

### 2026-05-29 — 타임스탬프 정책 통일

- **변경 내용**:
  - `auctions`: `deleted_at` 추가
  - `auction_images`: `updated_at`, `deleted_at` 추가
- **변경 이유**: 실무 보관 정책(최소 3년) 기준으로 도메인 엔티티 전체에 소프트 삭제 적용. `auction_results`는 로그형 테이블로 분류해 `created_at`만 유지
