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
| **역할** | 단일 도메인 비즈니스 로직 | 여러 Service 오케스트레이션 |
| **Repository** | 자기 도메인 + 필요 시 다른 도메인 Repository 직접 주입 가능 | Repository 직접 호출 금지 |
| **트랜잭션** | `@Transactional` 선언 | `@Transactional` 선언 (하위 Service 참여) |
| **생성 기준** | 항상 존재 | 2개 이상 도메인 Service의 상태 변경이 한 요청에서 발생할 때만 |

**다른 도메인 Repository 직접 주입 허용 이유:** 단순 엔티티 조회까지 Facade를 강제하면 Facade가 남발됨. 다른 도메인 Service 호출은 금지.

### Facade 생성 판단

```
이 요청에서 2개 이상 도메인의 상태가 변경되는가?
  YES → Facade 생성
  NO  → Service 단독 처리
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

### food

삭제: `FoodFinder`, `FoodManager`, `FoodModifier`, `FoodRemover`

`FoodService` 변경:
- 위 Executor 4개 로직 흡수
- `FoodRepository` + `CategoryRepository` + `MemberRepository` + `RefrigeratorRepository` 직접 주입

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
