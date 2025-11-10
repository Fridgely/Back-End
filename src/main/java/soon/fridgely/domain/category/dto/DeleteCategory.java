package soon.fridgely.domain.category.dto;

public record DeleteCategory(
    long memberId,
    long refrigeratorId,
    long categoryId
) {
}