# 네이밍 컨벤션 가이드

## 클래스 네이밍

| 유형 | 패턴 | 예시 |
|------|------|------|
| JPA 엔티티 | 명사 (접미사 없음) | `Food`, `Member`, `Refrigerator` |
| Controller | `{Domain}Controller` | `FoodController` |
| Controller Docs (Swagger) | `{Domain}ControllerDocs` | `FoodControllerDocs` |
| Service (Facade) | `{Domain}Service` | `FoodService` |
| Executor | `{Domain}{Role}` | `FoodFinder`, `FoodManager` |
| Repository | `{Domain}Repository` | `FoodRepository` |
| Request DTO | `{Action}{Domain}Request` | `FoodCreateRequest` |
| Response DTO | `{Domain}Response` | `FoodResponse`, `FoodDetailResponse` |
| Command (내부 전달) | 동사+명사 | `AddCategory`, `FoodCreateCommand` |
| 키 객체 | `{Domain}Key` | `MemberRefrigeratorKey` |
| 이벤트 | `{Domain}{Action}Event` | `RefrigeratorCreatedEvent` |
| 설정 | `{Name}Config` | `SecurityConfig` |
| 검증 | `{Domain}Validator` | `CategoryValidator` |

## 메서드 네이밍

### Controller
```java
append(...)      // POST (생성)
find(...)        // GET (단건 조회)
findAll(...)     // GET (목록 조회)
modify(...)      // PATCH (수정)
remove(...)      // DELETE (삭제)
```

### Finder (조회 전담)
```java
findByXxx(...)        // 단건 조회
findAllByXxx(...)     // 목록 조회
existsByXxx(...)      // 존재 확인
```

### Manager / Appender / Modifier / Remover
```java
register() / create() // 생성 (Entity 정적 팩토리와 동일 네이밍)
update() / modify()   // 수정
delete() / remove()   // 삭제
```

### DTO 변환
```java
request.toCommand()           // Request → Command (toCommand() 통일)
request.toCreateCommand(...)  // 생성용 커맨드
request.toUpdateCommand(...)  // 수정용 커맨드
Response.of(entity)           // Entity → Response (of() 통일, from() 금지)
Response.of(list)             // List 변환도 of() 사용
addCommand.toKey()            // Command → Key 변환
```

### Entity 상태 변경
```java
entity.register(...)   // 정적 팩토리 (생성)
entity.delete()        // 소프트 딜리트 (BaseEntity)
entity.updateName(name) // 명시적 수정 메서드
entity.add(amount)     // 도메인 의미 있는 동사
entity.consume(amount)
```

## 변수 네이밍

```java
// 단건 → 엔티티명
Food food = ...
Member member = ...

// 컬렉션 → 복수형
List<Food> foods = ...

// Map → XxxMap 접미사 통일
Map<Long, Category> categoryMap = ...
Map<FoodStatus, List<FoodResponse>> foodStatusMap = ...

// 현재 시간 → 항상 now
LocalDate now = LocalDate.now();

// 식별자 조합 → key
MemberRefrigeratorKey key = new MemberRefrigeratorKey(memberId, refrigeratorId);

// 상수 → UPPER_SNAKE_CASE
private static final String IMAGE_KEY_PREFIX = "images/";
```

## 테스트 메서드 네이밍

```java
// 반드시 한글로 작성
@Test
void 냉장고에_속한_팀원_목록을_조회한다() { }

@Test
void 냉장고에_속하지_않은_멤버는_예외가_발생한다() { }

@Test
void 기본_카테고리는_수정할_수_없다() { }
```
