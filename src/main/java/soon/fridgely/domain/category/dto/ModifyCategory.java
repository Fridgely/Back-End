package soon.fridgely.domain.category.dto;

public record ModifyCategory(
    long refrigeratorId,
    long memberId,
    long categoryId,
    String newName
) {
}