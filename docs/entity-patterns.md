# 엔티티 · DTO · 커맨드 설계 패턴

## 엔티티 설계

```java
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)  // Builder만 허용
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시용
@Getter                                             // Setter 없음
@Table(name = "categories",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_categories_refrigerator_id_name",
        columnNames = {"refrigerator_id", "name"}))
@Entity
public class Category extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)  // 항상 LAZY
    @JoinColumn(name = "refrigerator_id", nullable = false)
    private Refrigerator refrigerator;

    // 정적 팩토리 메서드 - 필수값 생성 시점 검증
    public static Category register(String name, Refrigerator refrigerator, Member member, CategoryType type) {
        return Category.builder()
            .name(requireNonNull(name, "name은 필수입니다."))
            .refrigerator(requireNonNull(refrigerator, "refrigerator는 필수입니다."))
            .member(requireNonNull(member, "member는 필수입니다."))
            .type(requireNonNull(type, "type은 필수입니다."))
            .build();
    }

    // 도메인 로직 캡슐화
    public boolean isDefaultType() { return this.type == CategoryType.DEFAULT; }
    public boolean isSameName(String name) { return this.name.equals(name); }

    // 상태 변경은 명시적 메서드로만
    public void updateName(String name) {
        if (name == null || name.isBlank()) throw new CoreException(ErrorType.INVALID_REQUEST);
        this.name = name;
    }
}
```

### BaseEntity 제공 메서드
```java
entity.delete()    // EntityStatus.DELETED (소프트 딜리트)
entity.active()    // EntityStatus.ACTIVE (복구)
entity.isActive()  // status == ACTIVE
entity.isDeleted() // status == DELETED
```

### @Embeddable 값 객체
```java
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quantity {
    @Column(name = "quantity_amount", precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_unit")
    private Unit unit;

    public static Quantity register(BigDecimal amount, Unit unit) {
        // 검증 + 생성
    }

    public Quantity plus(Quantity other) { ... }
    public Quantity minus(Quantity other) { ... }
}
```

## DTO 설계

### Request DTO
```java
public record FoodCreateRequest(
    @NotBlank(message = "음식 이름은 필수입니다.") String name,
    @NotNull Long categoryId,
    @Positive BigDecimal amount
) {
    // Request → Command 변환 메서드 (toCommand 통일)
    public FoodCreateCommand toCommand(String imageURL) {
        return new FoodCreateCommand(name, Quantity.register(amount, unit), categoryId, imageURL);
    }
}
```

### Response DTO
```java
public record FoodDetailResponse(long id, String name, String categoryName) {
    // of() 정적 팩토리 (from() 금지)
    public static FoodDetailResponse of(Food food, LocalDate now) {
        return new FoodDetailResponse(food.getId(), food.getName(), food.getCategory().getName());
    }

    // 목록 변환도 of()
    public static List<FoodDetailResponse> of(List<Food> foods, LocalDate now) {
        return foods.stream().map(f -> of(f, now)).toList();
    }
}
```

### Command (내부 전달용)
```java
// 불변 record, 검증 없이 데이터 운반
public record FoodCreateCommand(String name, Quantity quantity, long categoryId, String imageURL) {}

// 키 객체
public record MemberRefrigeratorKey(long memberId, long refrigeratorId) {}
```

## Repository 패턴

```java
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 단순 조건 → 파생 쿼리
    Optional<Category> findByIdAndRefrigeratorAndStatus(long id, Refrigerator refrigerator, EntityStatus status);

    // 복잡한 조건 / fetch join → @Query JPQL
    @Query("""
        SELECT c FROM Category c
        JOIN FETCH c.refrigerator r
        WHERE c.refrigerator = :refrigerator
        AND c.status = :status
        """)
    List<Category> findAllByRefrigeratorAndStatus(
        @Param("refrigerator") Refrigerator refrigerator,
        @Param("status") EntityStatus status
    );
}
```

**규칙:**
- 연관 엔티티 사용 시 반드시 `JOIN FETCH` (N+1 방지)
- 조회 조건에 `EntityStatus.ACTIVE` 항상 명시
- QueryDSL은 동적 조건 필요 시 `{Domain}RepositoryImpl` 에 구현

## 커서 기반 페이징

```java
// 요청: cursorId가 null이면 첫 페이지 (Long.MAX_VALUE 처리)
public record CursorPageRequest<S extends Enum<S>>(Long cursorId, Integer size, S sortBy) {}

// Repository: Slice 반환 (hasNext() 제공)
Slice<Food> findAllByCursor(long refrigeratorId, long cursorId, Pageable pageable);
```

## 낙관적 락 + Retry

```java
// Entity에 @Version 추가 (동시 수정 충돌 방지)
@Version
private Long version;

// Service에 Resilience4j Retry 적용
@Retry(name = "invitationCodeGeneration", fallbackMethod = "generateFallback")
public InvitationCodeResponse generateInvitationCode(MemberRefrigeratorKey key) { ... }
```
