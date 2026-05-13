# 테스트 작성 가이드

## 테스트 계층

| 테스트 | 상속 클래스 | 특징 |
|--------|-------------|------|
| 통합 테스트 | `IntegrationTestSupport` | `@SpringBootTest`, 실제 DB(H2), `@TruncateTables` |
| Controller 테스트 | `ControllerTestSupport` | `@WebMvcTest`, MockMvc, Security 필터 OFF |
| E2E 테스트 | `E2ETestSupport` | `TestRestTemplate`, 실제 JWT, 랜덤 포트 |
| Unit 테스트 | `@ExtendWith(MockitoExtension.class)` | Mock 사용, 빠른 실행 |

## 테스트 작성 패턴

### 통합 테스트 예시
```java
class CategoryAppenderIntegrationTest extends IntegrationTestSupport {

    @Autowired CategoryAppender categoryAppender;
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
    void 냉장고ID와_멤버ID를_받아_기본_카테고리들을_일괄_생성한다() {
        // given
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        // when
        categoryAppender.appendDefaultCategories(key);

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

        categoryAppender.appendCustomCategory(addCommand);

        // expected
        assertThatThrownBy(() -> categoryAppender.appendCustomCategory(addCommand))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.DUPLICATE_CATEGORY_NAME);
    }
}
```

### Controller 테스트 예시
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

        then(categoryService).should().appendCustomCategory(any());
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

## 테이블 초기화

`IntegrationTestSupport`에 포함된 `@TruncateTables` 어노테이션이 각 테스트 후 모든 테이블을 TRUNCATE한다.
`@Transactional` 롤백 대신 실제 커밋 동작을 검증하기 위한 의도적인 선택이다.
`@BeforeEach`에서 공통 픽스처를 세팅하면 된다.

## Service Unit 테스트 (InOrder 검증)

```java
@ExtendWith(MockitoExtension.class)
class CategoryServiceUnitTest {

    @InjectMocks CategoryService categoryService;
    @Mock CategoryAppender categoryAppender;
    @Mock CategoryFinder categoryFinder;

    @Test
    void 커스텀_카테고리를_추가한다() {
        // given
        var addCommand = new AddCategory("야채", 1L, 1L);

        // when
        categoryService.appendCustomCategory(addCommand);

        // then - 호출 순서 검증
        InOrder inOrder = inOrder(categoryAppender);
        then(categoryAppender).should(inOrder).appendCustomCategory(addCommand);
    }
}
```
