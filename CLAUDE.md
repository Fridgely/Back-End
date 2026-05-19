# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 아키텍처

**스택**: Spring Boot 3.5.7, Java 17, JPA/QueryDSL, MySQL 8 (로컬/테스트는 H2), Spring Security 6 + JWT, Resilience4j, Firebase FCM, AWS S3

**프로파일**: `local` (H2), `test` (H2 + 완화된 rate limit), `live` (MySQL + Prometheus)

### 패키지 구조

```
soon.fridgely/
├── domain/
│   ├── auth/         # 로그인, 토큰 갱신
│   ├── barcode/      # 바코드 상품 조회
│   ├── category/     # 식품 카테고리
│   ├── food/         # 식품 관리 + 유통기간 스케줄러
│   ├── member/       # 회원
│   ├── notification/ # FCM 푸시 알림
│   └── refrigerator/ # 냉장고 관리 + 초대 코드
└── global/
    ├── config/       # Spring 설정 빈
    ├── infra/        # AWS S3, Firebase
    ├── security/     # JWT 필터, @LoginMember, @ValidateRefrigeratorAccess AOP
    └── support/      # CoreException, ApiResponse, 로깅
```

각 도메인 내부 구조:
```
controller/ / dto/{command,request,response}/ / entity/ / repository/ / service/ / batch/ / event/ / validator/
```

### 의존성 방향

```
Controller → Service → Repository
Controller → Facade → Service → Repository
```

- Controller는 Service 또는 Facade만 호출
- Facade는 다음 두 경우에만 생성:
  1. 2개 이상 도메인 Service의 상태 변경이 한 요청에서 발생할 때
  2. 외부 시스템(S3 등) 오케스트레이션과 도메인 상태 변경이 함께 필요할 때 (트랜잭션 밖에서 외부 I/O를 처리하기 위해)
- Service는 자기 도메인 Repository + 필요 시 다른 도메인 Repository 직접 주입 가능
- Service가 다른 도메인 Service를 직접 호출하는 것은 금지 (크로스 도메인 상태 변경은 Facade 담당)
- 역방향 의존성 절대 금지

### 마이그레이션 중인 도메인 (Executor → Service+Facade)

food/refrigerator/member/notification 도메인은 현재 기존 Executor 패턴(`*Finder`, `*Manager`, `*Modifier`, `*Remover`, `*Appender`, `*Linker`, `*Generator`) 클래스가 남아 있다.
마이그레이션 진행 중이므로 기존 Executor 클래스를 수정하는 작업은 허용하되, 새 Executor 클래스를 추가하는 것은 금지한다.
마이그레이션 순서 및 전략은 [`docs/migration-service-facade.md`](docs/migration-service-facade.md) 참조.

**완료**: category (CategoryService 단일 서비스로 통합), food (FoodService + FoodFacade)
**진행 중**: refrigerator, member, notification

## 핵심 규칙

> 상세 내용은 [`docs/core-rules.md`](docs/core-rules.md) 참조

- **트랜잭션**: Service에 선언, 조회 메서드는 `readOnly=true`, Controller 선언 금지; Facade는 목적에 따라 선택 — 외부 I/O 분리 목적이면 생략, 크로스 도메인 원자성이 필요하면 선언
- **예외**: `CoreException(ErrorType.XXX)` 사용, `IllegalArgumentException` 금지
- **엔티티**: `BaseEntity` 상속, 정적 팩토리 `register()`/`create()`, 소프트 딜리트(`entity.delete()`), 삭제는 멱등성 보장
- **DTO**: 불변 `record`, `request.toCommand()` / `Response.of(entity)` 팩토리 통일
- **보안**: `@LoginMember`(Controller), `@ValidateRefrigeratorAccess`(Service)
- **이벤트**: `@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW`
- **로깅**: `[Domain] 메시지. (key=value)`, 중요 이벤트에 `SlackMarkers` 사용
- **테스트**: 한글 메서드명, BDD 구조, 테스트 간 독립성 보장 — 상세 규칙은 [`docs/testing-rules.md`](docs/testing-rules.md) 참조

## 상세 가이드 (온디맨드)

| 문서 | 내용 |
|------|------|
| `docs/naming-guide.md` | 클래스·메서드·변수 네이밍 컨벤션 |
| `docs/entity-patterns.md` | 엔티티·DTO·커맨드 설계 패턴 상세 |
| `docs/testing-guide.md` | 테스트 작성 패턴 및 FixtureMonkey 활용 |
| `docs/core-rules.md` | 트랜잭션·예외·엔티티·DTO·이벤트·로깅 상세 규칙 |
| `docs/testing-rules.md` | 테스트 원칙 및 Fixture 규칙 |
| `docs/migration-service-facade.md` | Service + Facade 마이그레이션 전략 및 순서 |
