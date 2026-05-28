package soon.fridgely.domain.refrigerator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.category.service.CategoryService;
import soon.fridgely.domain.food.service.FoodService;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.global.security.annotation.ValidateRefrigeratorAccess;

@RequiredArgsConstructor
@Service
public class RefrigeratorFacade {

    private final RefrigeratorService refrigeratorService;
    private final FoodService foodService;
    private final CategoryService categoryService;

    @ValidateRefrigeratorAccess(key = "#key")
    @Transactional
    public void deleteRefrigerator(MemberRefrigeratorKey key) {
        MemberRefrigerator memberRefrigerator = refrigeratorService.findMemberRefrigerator(key.memberId(), key.refrigeratorId());
        memberRefrigerator.validateOwnership();
        foodService.removeAllByRefrigeratorId(key.refrigeratorId());
        categoryService.removeAllByRefrigeratorId(key.refrigeratorId());
        refrigeratorService.unlinkAll(key.refrigeratorId());
        refrigeratorService.delete(key.refrigeratorId());
    }
}
