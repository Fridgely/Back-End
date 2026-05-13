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
Controller → Service → Executor(Finder/Manager/...) → Repository
```

- Controller는 Service만 호출 (Executor 직접 호출 금지)
- Service는 Repository 직접 호출 금지, 항상 Executor를 통해 접근
- 역방향 의존성 절대 금지

### Executor 패턴

Service는 역할별 컴포넌트에 위임한다. 기본적으로 `*Manager`를 사용하며, 특정 역할의 메서드가 2개 이상이 되면 해당 역할의 컴포넌트로 분리한다.

| 컴포넌트 | 역할 |
|----------|------|
| `*Manager` | 기본 컴포넌트 |
| `*Finder` | 조회 메서드가 2개 이상일 때 분리 |
| `*Modifier` | 수정 메서드가 2개 이상일 때 분리 |
| `*Appender` | 추가 메서드가 2개 이상일 때 분리 |
| `*Remover` | 삭제 메서드가 2개 이상일 때 분리 |
| `*Linker` | 연관관계 메서드가 2개 이상일 때 분리 |
| `*Generator` | 생성 알고리즘 메서드가 2개 이상일 때 분리 |
| `*Validator` | 검증 로직이 2개 이상일 때 분리 |

## 핵심 규칙

> 상세 내용은 [`docs/core-rules.md`](docs/core-rules.md) 참조

- **트랜잭션**: Service/Executor에 선언, `Finder`는 `readOnly=true`, Controller 선언 금지
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
