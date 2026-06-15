# Common Exception 설계 결정

작성일: 2026-06-15

---

## 도입 배경

초기에는 서비스 코드 내부에 `HttpStatus`와 메시지 문자열을 직접 하드코딩했다.
기능이 늘어날수록 같은 에러 코드를 여러 곳에 중복 작성하게 되고, 오타나 불일치가 생기기 쉬워서 별도 에러 코드 관리 구조로 정비했다.

---

## 구조 개요

```
common/exception/
├── BusinessException.java       ← 공통 예외 클래스 + ErrorCodeEnum 인터페이스
├── ErrorCode.java               ← 공통 에러 코드 Enum
└── GlobalExceptionHandler.java  ← @RestControllerAdvice 핸들러

domain/auth/exception/
└── AuthErrorCode.java           ← 인증 도메인 에러 코드 Enum

domain/user/exception/
└── UserErrorCode.java           (예정)
```

---

## 핵심 설계 결정

### 1. `BusinessException.ErrorCodeEnum` 인터페이스 패턴

```java
public interface ErrorCodeEnum {
    HttpStatus getHttpStatus();
    String getCode();
    String getMessage();
}
```

- 공통 `ErrorCode`와 도메인별 `AuthErrorCode`, `UserErrorCode` 등 **서로 다른 Enum**이 하나의 `BusinessException`을 공유할 수 있게 한다
- `throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS)` 형태로 어느 Enum이든 동일하게 사용 가능
- **이유**: Enum은 다중 상속이 안 되므로 공통 Enum 하나에 모든 코드를 넣는 대신 인터페이스로 계약을 정의하고 각 도메인이 독립적으로 구현한다

### 2. 에러 코드 파일 도메인 분리

- **공통 에러** (`ErrorCode.java`): HTTP 프로토콜 레벨 오류 (400, 405, 415, 500 등)
- **도메인 에러** (`AuthErrorCode`, `UserErrorCode` 등): 해당 도메인의 비즈니스 규칙 위반

**이유**:
- 공통 파일 하나에 전부 넣으면 파일이 길어지고 도메인 간 코드가 뒤섞여 가독성이 떨어진다
- 도메인 패키지 안에 두면 "이 에러는 인증 도메인 것이다"가 패키지 위치로도 명확해진다
- 다른 AI나 개발자가 인증 관련 에러를 찾을 때 `domain/auth/exception/`만 보면 된다

### 3. `details` 필드 — `List<String>` 사용

```java
private final List<String> details;
```

- 유효성 검증 실패 시 **여러 필드의 오류를 한 번에 응답**할 수 있다
  - 예: `["email: 이메일 형식이 아닙니다", "password: 필수 항목입니다"]`
- 단순 에러는 `details = null`로 처리하고, 필드 오류가 있을 때만 채운다
- `String`이 아닌 `List<String>`인 이유: 여러 필드 오류를 한 번의 응답으로 전달해 클라이언트가 개별 필드에 에러 메시지를 표시할 수 있게 함

---

## 변경 이력

### 2026-06-15 최초 작성

- 배경: 서비스 코드에 에러 문자열 하드코딩 → 에러 코드 Enum으로 정비
- `BusinessException` + `ErrorCodeEnum` 인터페이스 패턴 도입
- 공통 `ErrorCode` + 도메인별 분리 구조 확정
