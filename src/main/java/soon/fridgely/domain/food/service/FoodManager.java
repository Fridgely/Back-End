package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.service.CategoryFinder;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.repository.FoodRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDate;

@RequiredArgsConstructor
@Component
public class FoodManager {

    private final FoodRepository foodRepository;
    private final MemberRepository memberRepository;
    private final RefrigeratorRepository refrigeratorRepository;
    private final CategoryFinder categoryFinder;

    @Transactional
    public void createFood(FoodInfo info, MemberRefrigeratorKey key, long categoryId) {
        Member member = memberRepository.findByIdAndStatus(key.memberId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        Refrigerator refrigerator = refrigeratorRepository.findByIdAndStatus(key.refrigeratorId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        Category category = categoryFinder.findByRefrigerator(categoryId, key.refrigeratorId());

        Food food = info.toEntity(member, refrigerator, category, LocalDate.now());
        foodRepository.save(food);
    }

    @Transactional
    public void delete(long foodId, long refrigeratorId) {
        Food food = foodRepository.findByIdAndRefrigeratorId(foodId, refrigeratorId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));

        if (food.isDeleted()) {
            return;
        }

        food.delete();
    }

}