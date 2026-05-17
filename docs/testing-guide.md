# 테스트 작성 가이드

## 테스트 전략

유지보수 비용과 효용을 기준으로 세 계층에 집중한다.

- **단위 테스트**: Entity 내부의 순수 비즈니스 정책 검증. Spring 없이 빠르게 실행.
- **통합 테스트**: Service 유스케이스를 실제 DB + 실제 객체로 검증. 계층 전체를 한 번에 커버. **가장 중심**.
- **E2E 테스트**: API 엔드포인트를 사용자 관점에서 블랙박스 검증. 핵심 시나리오 20%에만 작성.

모든 API를 커버하는 것이 목표가 아니다. 버그 가능성이 높고 변경이 잦은 핵심 기능에 집중한다.

## 테스트 계층

| 테스트 | 상속 클래스 | 언제 쓰나 |
|--------|-------------|-----------|
| 단위 테스트 | `@ExtendWith(MockitoExtension.class)` | Entity 도메인 정책, 외부 의존 없는 순수 로직 |
| 통합 테스트 | `IntegrationTestSupport` | Service 유스케이스, 실제 DB(H2) + 실제 객체 |
| E2E 테스트 | `E2ETestSupport` | 핵심 API 시나리오, 사용자 관점 블랙박스 |
| Controller 테스트 | `ControllerTestSupport` | 요청 직렬화/역직렬화, 응답 포맷 검증에 한정 |

## 테스트 작성 패턴

### 통합 테스트 예시

Service 메서드 단위로 작성한다. "어떤 순서로 호출됐는지"가 아닌 **"DB 상태가 올바른지"** 를 검증한다.

```java
class CategoryServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired CategoryService categoryService;
    @Autowired MemberRepository memberRepository;
    @Autowired RefrigeratorRepository refrigeratorRepository;
    @Autowired CategoryRepository categoryRepository;

    private Member member;
    private Refrigerator refrigerator;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(member(fixtureMonkey).sample());
        refrigerator = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());
    }

    @Test
    void 냉장고_생성_시_기본_카테고리_8개가_생성된다() {
        // given
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        // when
        categoryService.appendDefaultCategories(key);

        // then
        List<Category> categories = categoryRepository
            .findAllByRefrigeratorAndStatus(refrigerator, EntityStatus.ACTIVE);

        assertThat(categories).hasSize(8)
            .extracting("type")
            .containsOnly(CategoryType.DEFAULT);
    }

    @Test
    void 동일한_냉장고에_중복된_이름의_카테고리를_추가하면_예외가_발생한다() {
        // given
        var addCommand = fixtureMonkey.giveMeBuilder(AddCategory.class)
            .set("name", "야채")
            .set("refrigeratorId", refrigerator.getId())
            .set("memberId", member.getId())
            .sample();

        categoryService.appendCustomCategory(addCommand);

        // expected
        assertThatThrownBy(() -> categoryService.appendCustomCategory(addCommand))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.DUPLICATE_CATEGORY_NAME);
    }
}
```

### Controller 테스트 예시

요청 직렬화/역직렬화, HTTP 상태 코드, 응답 포맷 검증에 한정한다.
비즈니스 로직 검증은 통합 테스트에서 담당하므로 Service 호출 순서 검증(`then(service).should(...)`)은 작성하지 않는다.

```java
class CategoryControllerTest extends ControllerTestSupport {

    @Test
    void 카테고리를_추가한다() throws Exception {
        // given
        var request = new CategoryAddRequest("야채");

        // when & then
        mockMvc.perform(post("/api/v1/refrigerators/1/categories")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }
}
```

### E2E 테스트 예시
```java
class CategoryControllerE2ETest extends E2ETestSupport {

    @Test
    void 카테고리_추가_후_목록에서_조회된다() {
        // given
        Member member = memberRepository.save(member(fixtureMonkey).sample());
        Refrigerator refrigerator = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());
        HttpHeaders headers = createAuthHeaders(member);

        var request = new CategoryAddRequest("야채");

        // when
        ResponseEntity<ApiResponse<?>> response = testRestTemplate.exchange(
            "/api/v1/refrigerators/" + refrigerator.getId() + "/categories",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            new ParameterizedTypeReference<>() {}
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

## FixtureMonkey 활용

```java
// 기본 생성 (랜덤 값)
var food = fixtureMonkey.giveMeOne(Food.class);

// 특정 필드 지정
var food = fixtureMonkey.giveMeBuilder(Food.class)
    .set("name", "우유")
    .set("status", EntityStatus.ACTIVE)
    .setNull("id")   // JPA 자동 생성 필드는 null
    .sample();
```

## Fixture 헬퍼

`src/test/java/soon/fridgely/global/support/fixture/` 에 도메인별 Fixture가 있다.

```java
// 사용 예시
Member member = memberRepository.save(member(fixtureMonkey).sample());
Refrigerator fridge = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());
MemberRefrigerator mr = memberRefrigeratorRepository.save(
    memberRefrigerator(fixtureMonkey, fridge, member)
        .set("role", RefrigeratorRole.OWNER)
        .sample()
);
```

새 도메인 추가 시 반드시 Fixture 헬퍼를 작성한다.

## 캐시 통합 테스트 실행 조건

`CategoryCacheIntegrationTest`, `RefrigeratorCacheIntegrationTest` 는 Redis에 의존한다.
Docker가 실행 중이지 않으면 컨텍스트 로딩 단계에서 실패하므로, 로컬에서 전체 테스트를 돌리기 전에 Docker를 먼저 기동해야 한다.

```bash
docker compose up -d redis
```

## 테이블 초기화

`IntegrationTestSupport`에 포함된 `@TruncateTables` 어노테이션이 각 테스트 후 모든 테이블을 TRUNCATE한다.
`@Transactional` 롤백 대신 실제 커밋 동작을 검증하기 위한 의도적인 선택이다.
`@BeforeEach`에서 공통 픽스처를 세팅하면 된다.

## Mock 사용 기준

내부 구현을 Mock으로 검증하면 리팩토링 시 테스트가 줄줄이 깨진다 (Fragile Test). Mock은 아래 기준에서만 사용한다.

| 대상 | 처리 방식 |
|---|---|
| S3, FCM 등 외부 서비스 | Fake 객체로 대체 |
| 알림 발송 등 부수 효과만 있는 경우 | Dummy |
| 내부 비즈니스 로직 | **실제 객체 사용** |

**하지 말아야 할 패턴 — Service 내부 호출 순서 검증:**

```java
// 내부 구현이 바뀌면 즉시 깨짐. 비즈니스 가치 없음
InOrder inOrder = inOrder(someComponent);
then(someComponent).should(inOrder).doSomething(...);
```

**대신 — 비즈니스 결과(DB 상태) 검증:**

```java
// when
foodService.createFood(request, file, key);

// then - DB에서 직접 확인
Food saved = foodRepository.findById(...).orElseThrow();
assertThat(saved.getName()).isEqualTo(request.name());
```

**Fake 객체 예시 (외부 서비스 대체):**

```java
class FakeImageManager implements ImageManager {
    public boolean deleteWasCalled = false;

    @Override
    public String upload(MultipartFile file) { return "http://fake/image.jpg"; }

    @Override
    public void delete(String url) { deleteWasCalled = true; }
}

@Test
void DB_저장_실패_시_이미지를_삭제한다() {
    // given - FakeImageManager 주입 후 저장 실패 상황 세팅

    // then
    assertThat(fakeImageManager.deleteWasCalled).isTrue();
}
```
