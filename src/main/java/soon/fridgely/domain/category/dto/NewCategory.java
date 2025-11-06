package soon.fridgely.domain.category.dto;

public record NewCategory(
    String name,
    long refrigeratorId,
    long memberId
) {
}