# Service + Facade 마이그레이션 가이드

## 변경 개요

Executor 패턴(Finder/Manager/Modifier 등) → Service + Facade 패턴으로 전환

**변경 이유:** 현재 규모에서 Executor 계층이 불필요한 보일러플레이트를 생성. 도메인당 4~5개 Executor → 1개 Service로 단순화.

---

## 새 아키텍처

### 의존성 방향

```
단순 케이스:   Controller → Service → Repository
크로스 도메인: Controller → Facade → Service → Repository
```

### Service vs Facade 기준

| | Service | Facade |
|---|---|---|
| **역할** | 단일 도메인 비즈니스 로직 | 여러 Service 오케스트레이션 또는 외부 시스템 + DB 분리 |
| **Repository** | 자기 도메인 + 필요 시 다른 도메인 Repository 직접 주입 가능 | Repository 직접 호출 금지 |
| **트랜잭션** | `@Transactional` 선언 | 목적에 따라 선택 (아래 참조) |
| **생성 기준** | 항상 존재 | 아래 두 경우 중 하나에 해당할 때만 |

**다른 도메인 Repository 직접 주입 허용 이유:** 단순 엔티티 조회까지 Facade를 강제하면 Facade가 남발됨. 다른 도메인 Service 호출은 금지.

**Facade 트랜잭션 선택 기준:**
- 외부 I/O 분리 목적(`FoodFacade` 케이스): `@Transactional` 없음 — S3 호출을 트랜잭션 밖으로 분리하는 것이 목적이므로 선언하면 안 됨
- 크로스 도메인 원자성 목적(`RefrigeratorFacade` 케이스): `@Transactional` 선언 — 하위 Service 트랜잭션이 전파(REQUIRED)로 참여하여 전체 롤백 보장

### Facade 생성 판단

```text
다음 중 하나라도 해당되는가?
  1. 이 요청에서 2개 이상 도메인 Service의 상태가 변경되는가?
  2. 외부 시스템(S3, Firebase 등) I/O와 DB 트랜잭션을 분리해야 하는가?
     (트랜잭션 안에서 외부 네트워크 호출이 발생하면 커넥션 점유 위험)

  YES → Facade 생성
  NO  → Service 단독 처리
```

**외부 시스템 Facade 패턴 (트랜잭션 없음):**
```text
Facade (@Transactional 없음):
  1. 외부 시스템 호출 (S3 업로드 등)
  2. Service 호출 (@Transactional) — DB 저장만
  3. catch: 외부 시스템 롤백 (S3 삭제 등)
```

**크로스 도메인 Facade 패턴 (@Transactional 선언):**
```text
Facade (@Transactional):
  → ServiceA.methodA()   ┐
  → ServiceB.methodB()   ├ 동일 트랜잭션 참여 (REQUIRED)
  → ServiceC.methodC()   ┘
  ※ 하나라도 실패하면 전체 롤백
```

---

## 마이그레이션 순서

의존성 방향 기준으로 leaf(의존받는 쪽)부터 진행:

```
1. category     ← 다른 도메인에서 가장 많이 참조, 자체 외부 의존 없음
2. food         ← CategoryService 완료 후
3. refrigerator ← FoodService + CategoryService 완료 후, Facade 생성
4. member       ← 독립적, 순서 무관
5. notification ← 독립적, 순서 무관
```

---

## 도메인별 변환 상세

### category

삭제: `CategoryAppender`, `CategoryFinder`, `CategoryModifier`, `CategoryRemover`

`CategoryService` 변경:
- 위 Executor 4개 로직 흡수
- `CategoryRepository` + `MemberRepository` + `RefrigeratorRepository` 직접 주입

### food ✅ 완료

삭제: `FoodFinder`, `FoodManager`, `FoodModifier`, `FoodRemover`

`FoodService` 변경:
- 위 Executor 4개 로직 흡수
- `FoodRepository` + `CategoryRepository` + `MemberRepository` + `RefrigeratorRepository` 직접 주입
- S3 업로드 로직 제거 → `FoodFacade`로 이전

`FoodFacade` 신규:
- S3 이미지 업로드 + `FoodService` 트랜잭션 오케스트레이션
- `createFood()`, `updateFood()` 담당 (외부 시스템 I/O + DB 분리)

### refrigerator

삭제: `RefrigeratorManager`, `MemberRefrigeratorFinder`, `MemberRefrigeratorLinker`, `InvitationCodeGenerator`

`RefrigeratorService` 변경:
- 위 Executor들 로직 흡수
- `deleteRefrigerator()` 는 Facade로 이동

`RefrigeratorFacade` 신규:
- `deleteRefrigerator()` 담당 (FoodService + CategoryService + RefrigeratorService 오케스트레이션)

### member / notification

각 Executor를 Service로 흡수. Facade 불필요.

---

## 테스트 마이그레이션

도메인 1개 기준 작업 순서:

```
1. Service에 Executor 로직 흡수
2. Executor 파일 삭제 (컴파일 에러 발생)
3. 컴파일 에러 난 Executor 테스트 → ServiceIntegrationTest로 케이스 이관
4. Mock 위주 ServiceUnitTest 삭제
5. 전체 테스트 실행 → 통과 확인
6. PR
```

Executor 통합 테스트 → ServiceIntegrationTest 이관 매핑:

| 삭제 | 이관 대상 |
|---|---|
| `CategoryAppenderIntegrationTest` | `CategoryServiceIntegrationTest` |
| `CategoryFinderIntegrationTest` | `CategoryServiceIntegrationTest` |
| `CategoryModifierIntegrationTest` | `CategoryServiceIntegrationTest` |
| `CategoryRemoverIntegrationTest` | `CategoryServiceIntegrationTest` |
| `FoodFinderIntegrationTest` | `FoodServiceIntegrationTest` |
| `FoodManagerIntegrationTest` | `FoodServiceIntegrationTest` |
| `FoodModifierIntegrationTest` | `FoodServiceIntegrationTest` |
| `FoodModifierUnitTest` | `FoodServiceIntegrationTest`로 전환 |
| `FoodRemoverUnitTest` | `FoodServiceIntegrationTest`로 전환 |
| `FoodServiceUnitTest` (Mock 위주) | 삭제 |
