# Pickz Docs Index

## 개요

이 문서는 Pickz 설계/구현 문서의 진입점입니다.  
포트폴리오 관점에서 "무엇을 왜 선택했는지"를 단계별로 추적할 수 있도록 구성합니다.

## 설계 문서

- `2026-04-20_초기_설계.md`
  - 프로젝트 목표, MVP 범위, 비기능 요구사항
- `2026-04-20_테이블_설계.md`
  - 테이블 구조, 컬럼 설명, 설계 이유, 동시성 전략
- `src/main/java/com/yoon/pickz/common/API_공통_정책_초안.md`
  - Base URL, 버전 정책, 시간/금액/인증/응답 포맷 공통 기준
- `src/main/java/com/yoon/pickz/domain/auth/entity/API_인증_명세_초안.md`
  - 회원가입/로그인/토큰 재발급/로그아웃 명세

## 도메인 의사결정 문서 (구현 근처)

- `src/main/java/com/yoon/pickz/domain/user/entity/ENTITY_DECISION.md`
- `src/main/java/com/yoon/pickz/domain/auction/entity/ENTITY_DECISION.md`
- `src/main/java/com/yoon/pickz/domain/bid/entity/ENTITY_DECISION.md`
- `src/main/java/com/yoon/pickz/domain/auth/entity/ENTITY_DECISION.md`

## 문서 작성 규칙

- 각 문서는 다음 4가지를 포함한다.
  - 배경
  - 선택지
  - 결정
  - 검증
- 정책 변경 시 "무엇이 변경되었는지"와 "변경 이유"를 함께 기록한다.
- 성능/동시성 관련 내용은 가능하면 수치(응답시간, 충돌률, 실패율)로 남긴다.

## 다음 작성 예정 문서

- API 명세 문서
- 입찰 시퀀스/흐름 문서
- 동시성 테스트 결과 문서
- 트러블슈팅 로그 문서
