# Pickz

Pickz는 한정판 굿즈를 대상으로 한 실시간 경매 서비스입니다.  
이 프로젝트는 단순 CRUD 구현이 아니라, **동시 입찰 정합성**과 **확장 가능한 아키텍처 설계**를 중심으로 백엔드 역량을 증명하는 것을 목표로 합니다.

## 프로젝트 목표

- 실시간 경매 환경에서 동시 입찰 충돌을 안전하게 처리한다.
- Scale-out 환경을 고려해 서버 간 상태 공유 전략을 설계한다.
- 기술 선택의 이유와 트레이드오프를 문서로 남겨 실무형 의사결정 과정을 보여준다.

## 핵심 기술 스택

- Backend: Java 17/21, Spring Boot 3.x, Spring Data JPA
- Database: MySQL
- Realtime/Concurrency: Redis(Redisson), WebSocket
- Security: Spring Security, JWT
- Architecture: Clean Architecture + DDD

## 문제 정의

일반적인 경매 서비스에서는 다음 문제가 자주 발생합니다.

- 종료 직전 다중 입찰로 인한 경합 증가
- 다중 서버 환경에서 상태 불일치 위험
- 조회/쓰기 트래픽 증가 시 DB 병목 가능성

Pickz는 위 문제를 단순 기능 구현이 아닌 아키텍처/도메인 설계로 해결하는 과정을 포트폴리오로 기록합니다.

## 설계 원칙

- **정본 데이터는 DB 기준**: 입찰 이력/낙찰 결과는 DB에 보관
- **Redis는 보조 계층**: 실시간성 및 병목 완화 목적
- **초기 단순화**: 제약은 UNIQUE 최소 적용, 인덱스는 모니터링 후 점진 도입
- **동시성 전략**: 초기 낙관적 락 + 테스트 기반 비관적/분산락 검토

## 도메인 구성 (DDD)

- `user`: 사용자/판매자/업체 계정 경계
- `auction`: 경매 본체, 이미지, 종료 결과
- `bid`: 입찰 이력 정본 데이터
- `auth`: 리프레시 토큰 관리

도메인별 설계 근거 문서는 아래 링크에서 확인할 수 있습니다.

- `src/main/java/com/yoon/pickz/domain/user/entity/ENTITY_DECISION.md`
- `src/main/java/com/yoon/pickz/domain/auction/entity/ENTITY_DECISION.md`
- `src/main/java/com/yoon/pickz/domain/bid/entity/ENTITY_DECISION.md`
- `src/main/java/com/yoon/pickz/domain/auth/entity/ENTITY_DECISION.md`

## 실무형 진행 방식

Pickz는 아래 단계로 진행합니다.

1. 요구사항 정의
2. 도메인/ERD 설계
3. API 명세
4. 핵심 로직 구현(입찰/종료)
5. 동시성/부하 테스트
6. 결과 기반 아키텍처 보완

각 단계에서 다음 형식으로 의사결정을 기록합니다.

- 배경: 어떤 문제가 있었는가
- 선택지: 고려한 대안은 무엇인가
- 결정: 왜 이 방식을 선택했는가
- 검증: 어떤 테스트/지표로 확인했는가

## 문서

- 설계 문서 인덱스: `docs/INDEX.md`
- 초기 설계: `docs/2026-04-20_초기_설계.md`
- 테이블 설계: `docs/2026-04-20_테이블_설계.md`

## 현재 상태

- [x] 프로젝트 방향/요구사항 초안
- [x] 도메인 및 테이블 설계 초안
- [x] 도메인별 엔티티 설계 근거 문서화
- [ ] API 상세 명세
- [ ] 핵심 입찰 로직 구현
- [ ] 동시성/부하 테스트 및 결과 정리
