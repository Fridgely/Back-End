# 핵심 코딩 규칙

## 트랜잭션

- `@Transactional` / `@Transactional(readOnly = true)` 선언 위치: Service 또는 Executor
- 조회 메서드(`Finder`)는 반드시 `@Transactional(readOnly = true)`
- Controller에 `@Transactional` 절대 금지
- `@ValidateRefrigeratorAccess` AOP는 트랜잭션 시작 전 실행 (Order=100)

## 예외 처리

- 모든 비즈니스 예외는 `CoreException(ErrorType.XXX)` 사용
- `ErrorType` LogLevel: 시스템 오류 → `ERROR`, 클라이언트 입력/비즈니스 위반 → `INFO`, 인증/권한 → `WARN`

## 엔티티

- 모든 엔티티는 `BaseEntity` 상속 (ID, createdAt, modifiedAt, EntityStatus)
- 생성은 정적 팩토리 메서드 `register()` 또는 `create()`로만 허용 (`@Builder` private)
- Setter 사용 금지, 상태 변경은 명시적 메서드(`delete()`, `updateName()`)로만
- 물리 삭제 대신 `entity.delete()` 소프트 딜리트, 조회 시 `EntityStatus.ACTIVE` 조건 명시
- 삭제 메서드는 항상 멱등성 보장: `findById`로 조회 후 이미 `DELETED` 상태이면 early return (상태와 무관하게 존재하지 않을 때만 `NOT_FOUND_DATA` 예외)

## DTO

- 모든 DTO는 불변 `record` 사용
- Request → Command 변환: `request.toCommand()` (메서드명 통일)
- Entity → Response 변환: `Response.of(entity)` (팩토리 메서드 `of()` 통일, `from()` 금지)

## 보안

- `@LoginMember`: Controller에서 인증된 회원 ID 자동 주입
- `@ValidateRefrigeratorAccess(key = "#key")`: Service 메서드에 선언, SpEL로 `MemberRefrigeratorKey` 지정

## 도메인 이벤트

- 후속/비동기 작업은 이벤트로 분리 (예: 냉장고 생성 → 기본 카테고리 생성)
- 이벤트 리스너: `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Transactional(propagation = REQUIRES_NEW)`
- 동기적 데이터 조회가 필요한 경우는 직접 의존 사용

## 로깅

- 형식: `[Domain] 메시지. (key=value)` — 예: `log.debug("[Auth] 로그인 성공. (MemberId={})", id)`
- 중요 이벤트에 SlackMarkers 사용: `SYSTEM`(보안/데이터 이상), `BATCH`(배치 결과), `BUSINESS`(주요 이벤트)
