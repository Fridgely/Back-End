package soon.fridgely.domain.category.dto.command;

public record DeleteCategory(
    long memberId,
    long refrigeratorId,
    long categoryId
) {
}