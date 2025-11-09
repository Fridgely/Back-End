package soon.fridgely.domain.category.dto;

public record ModifyCategory(
    String newName,
    long memberId,
    long refrigeratorId,
    long categoryId
) {
}