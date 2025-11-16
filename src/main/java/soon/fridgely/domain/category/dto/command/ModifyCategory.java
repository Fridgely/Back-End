package soon.fridgely.domain.category.dto.command;

public record ModifyCategory(
    String newName,
    long memberId,
    long refrigeratorId,
    long categoryId
) {
}