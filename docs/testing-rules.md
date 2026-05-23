# 테스트 작성 규칙

## 선택적 작성 원칙

- 모든 코드에 테스트를 작성하지 않는다. 버그 가능성이 높고 변경이 잦은 핵심 기능에 집중한다.
- 수명이 짧은 이벤트성 기능은 수동 테스트로 대체할 수 있다.
- 커버리지 수치가 목표가 아니라, 시스템 신뢰성과 문서화 역할을 하는 테스트가 목표다.
- 상세 전략은 [`docs/testing-guide.md`](testing-guide.md) 참조

## 테스트 메서드명

- 명사 나열이 아닌 문장으로 작성한다 (A이면 B이다 / A이면 B가 아니고 C다)
- `XXX 테스트` 형태의 제목을 지양한다
- 행위의 결과까지 기술한다
  - ❌ `음료를 1개 추가할 수 있다`
  - ✅ `음료를 1개 추가하면 주문 목록에 담긴다`
- 도메인 용어를 사용해 정책 관점으로 기술한다
  - ❌ `특정 시간 이전에 주문을 생성하면 실패한다`
  - ✅ `영업 시작 시간 이전에는 주문을 생성할 수 없다`
- 메서드 구현 관점이 아닌 도메인 정책·현상 중심으로 기술한다

## 테스트 구조

- BDD 기반으로 작성한다
- `// given`, `// when`, `// then` 주석으로 구조를 명시적으로 분리한다
- 한 테스트에는 한 가지 주제만 검증한다
- 각 테스트 간 독립성을 보장한다 (공유 변수·공유 객체 사용 금지)

## Test Fixture

- given 데이터는 한 눈에 파악할 수 있게 구성한다
- 아래 두 조건을 모두 만족하면 `@BeforeEach` setup으로 공유해도 된다:
  1. fixture를 몰라도 각 테스트 내용을 이해하는 데 문제가 없다
  2. fixture를 수정해도 모든 테스트에 영향을 주지 않는다
- 위 조건을 만족하지 않으면 각 테스트마다 독립적으로 생성한다

## 검증 패턴

동일한 대상의 여러 필드를 검증할 때는 `assertThat`을 반복하는 대신 `extracting`으로 한 번에 뽑아 검증한다.

```java
// ❌ 반복 assertThat
assertThat(response.blackCount()).isEqualTo(2);
assertThat(response.redCount()).isEqualTo(3);
assertThat(response.yellowCount()).isEqualTo(1);

// ✅ extracting으로 한 번에
assertThat(response)
    .extracting(
        FoodStatusResponse::blackCount,
        FoodStatusResponse::redCount,
        FoodStatusResponse::yellowCount
    )
    .containsExactly(2, 3, 1);
```

컬렉션 요소 전체에 동일 조건이 성립하는 경우:

```java
// ❌ 반복 assertThat
assertThat(result.get(FoodStatus.BLACK)).isEmpty();
assertThat(result.get(FoodStatus.RED)).isEmpty();
assertThat(result.get(FoodStatus.YELLOW)).isEmpty();

// ✅ extracting + containsOnly
assertThat(List.of(FoodStatus.BLACK, FoodStatus.RED, FoodStatus.YELLOW))
    .extracting(status -> result.get(status).size())
    .containsOnly(0);
```

## 테스트 클래스 종류

| 클래스 | 용도 |
|--------|------|
| `IntegrationTestSupport` | 통합 테스트 (`@TruncateTables`로 DB 초기화) |
| `ControllerTestSupport` | Controller 테스트 (MockMvc, Security 필터 비활성화) |
| `AcceptanceTestSupport` | 인수 테스트 (TestRestTemplate + 실제 JWT 토큰) |

테스트 데이터 생성: FixtureMonkey + `src/test/.../fixture/` 의 도메인별 Fixture 헬퍼
