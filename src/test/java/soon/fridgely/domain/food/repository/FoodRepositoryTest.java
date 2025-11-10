package soon.fridgely.domain.food.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.entity.CategoryType;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.food.entity.*;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.entity.MemberRole;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.config.JpaAuditingConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Import(JpaAuditingConfig.class)
@DataJpaTest
class FoodRepositoryTest {

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager em;

    @Test
    void 대상_카테고리의_모든_음식을_폴백_카테고리로_이동한다() {
        Member member = createMember();
        memberRepository.save(member);

        Refrigerator refrigerator = Refrigerator.register(member.getNickname());
        refrigeratorRepository.save(refrigerator);

        Category targetCategory = Category.register("삭제될 카테고리", refrigerator, member, CategoryType.CUSTOM);
        Category fallbackCategory = Category.register("기타", refrigerator, member, CategoryType.DEFAULT);
        categoryRepository.saveAll(List.of(targetCategory, fallbackCategory));

        List<Food> foods = Stream.generate(() -> createFood(refrigerator, member, targetCategory))
            .limit(3)
            .collect(Collectors.toList());
        foodRepository.saveAll(foods);

        foodRepository.moveAllFoodsToFallbackCategory(targetCategory, fallbackCategory);
        em.clear();

        List<Food> updatedFoods = foodRepository.findAll();
        assertThat(updatedFoods)
            .hasSize(3)
            .allSatisfy(f -> assertThat(f.getCategory().getName())
                .isEqualTo("기타"));
    }

    private Member createMember() {
        return Member.builder()
            .loginId("testId")
            .password("testPassword")
            .nickname("testNickname")
            .role(MemberRole.MEMBER)
            .build();
    }

    private Food createFood(Refrigerator refrigerator, Member member, Category category) {
        return Food.register(
            refrigerator,
            member,
            "testFood",
            category,
            new Quantity(new BigDecimal("1.0"), Unit.KG),
            LocalDateTime.now().plusDays(2L),
            StorageType.FROZEN,
            FoodStatus.GREEN,
            "testDescription",
            "http://example.com/image.jpg"
        );
    }

}