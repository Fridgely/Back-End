package soon.fridgely.domain.category.dto;

public record AddCategory(
    String name,
    long refrigeratorId,
    long memberId
) {
}