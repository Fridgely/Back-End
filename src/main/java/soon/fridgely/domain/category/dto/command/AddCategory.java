package soon.fridgely.domain.category.dto.command;

public record AddCategory(
    String name,
    long refrigeratorId,
    long memberId
) {
}