package soon.fridgely.domain.category.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import soon.fridgely.domain.category.dto.command.AddCategory;
import soon.fridgely.domain.category.dto.command.DeleteCategory;
import soon.fridgely.domain.category.dto.command.ModifyCategory;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.category.repository.CategoryRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

@TestPropertySource(properties = "spring.cache.type=caffeine")
class CategoryCacheIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private CategoryFinder categoryFinder;

    @Autowired
    private CategoryAppender categoryAppender;

    @Autowired
    private CategoryModifier categoryModifier;

    @Autowired
    private CategoryRemover categoryRemover;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private CacheManager cacheManager;

    private Member member;
    private Refrigerator refrigerator;

    @BeforeEach
    void setUp() {
        this.member = memberRepository.save(
            member(fixtureMonkey).sample()
        );
        this.refrigerator = refrigeratorRepository.save(
            refrigerator(fixtureMonkey).sample()
        );

        Cache cache = cacheManager.getCache("categories");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void 카테고리_목록_조회_시_캐시가_적용된다() {
        // given
        MemberRefrigeratorKey key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());
        categoryAppender.appendDefaultCategories(key);

        // when
        List<Category> firstResult = categoryFinder.findAll(refrigerator.getId());
        assertThat(firstResult).hasSize(8);

        Cache cache = cacheManager.getCache("categories");
        assertThat(cache).isNotNull();
        assertThat(cache.get(refrigerator.getId())).isNotNull();


        List<Category> secondResult = categoryFinder.findAll(refrigerator.getId()); // 같은 인스턴스 반환
        // then
        assertThat(secondResult).hasSize(8);
        assertThat(firstResult).isEqualTo(secondResult);
    }

    @Test
    void 커스텀_카테고리_추가_시_캐시가_무효화된다() {
        // given
        MemberRefrigeratorKey key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());
        categoryAppender.appendDefaultCategories(key);

        List<Category> cachedResult = categoryFinder.findAll(refrigerator.getId());
        assertThat(cachedResult).hasSize(8);

        // when
        AddCategory addCategory = new AddCategory("새 카테고리", refrigerator.getId(), member.getId());
        categoryAppender.appendCustomCategory(addCategory);

        // then
        List<Category> updatedResult = categoryFinder.findAll(refrigerator.getId());
        assertThat(updatedResult).hasSize(9)
            .extracting(Category::getName)
            .contains("새 카테고리");
    }

    @Test
    void 기본_카테고리_생성_시_캐시가_무효화된다() {
        // given
        MemberRefrigeratorKey key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        // when
        categoryAppender.appendDefaultCategories(key);

        Cache cache = cacheManager.getCache("categories");
        assertThat(cache.get(refrigerator.getId())).isNull();

        List<Category> categories = categoryFinder.findAll(refrigerator.getId());

        // then
        assertThat(categories).hasSize(8)
            .extracting(Category::getName)
            .containsExactlyInAnyOrder("야채", "과일", "육류", "해산물", "유제품", "음료", "간식", "기타");
    }

    @Test
    void 카테고리_수정_시_캐시가_무효화된다() {
        // given
        MemberRefrigeratorKey key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());
        categoryAppender.appendDefaultCategories(key);

        AddCategory addCategory = new AddCategory("수정될 카테고리", refrigerator.getId(), member.getId());
        categoryAppender.appendCustomCategory(addCategory);

        List<Category> cachedCategories = categoryFinder.findAll(refrigerator.getId());
        Category targetCategory = cachedCategories.stream()
            .filter(c -> c.getName().equals("수정될 카테고리"))
            .findFirst()
            .orElseThrow();

        // when
        ModifyCategory modifyCategory = new ModifyCategory(
            "수정된 카테고리",
            member.getId(),
            refrigerator.getId(),
            targetCategory.getId()
        );
        categoryModifier.modify(modifyCategory);

        // then
        List<Category> updatedCategories = categoryFinder.findAll(refrigerator.getId());
        assertThat(updatedCategories)
            .extracting(Category::getName)
            .contains("수정된 카테고리")
            .doesNotContain("수정될 카테고리");
    }

    @Test
    void 카테고리_삭제_시_캐시가_무효화된다() {
        // given
        MemberRefrigeratorKey key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());
        categoryAppender.appendDefaultCategories(key);
        AddCategory addCategory = new AddCategory("삭제될 카테고리", refrigerator.getId(), member.getId());
        categoryAppender.appendCustomCategory(addCategory);

        List<Category> cachedCategories = categoryFinder.findAll(refrigerator.getId());
        Category targetCategory = cachedCategories.stream()
            .filter(c -> c.getName().equals("삭제될 카테고리"))
            .findFirst()
            .orElseThrow();

        assertThat(cachedCategories).hasSize(9);

        // when - 카테고리 삭제
        DeleteCategory deleteCategory = new DeleteCategory(
            member.getId(),
            refrigerator.getId(),
            targetCategory.getId()
        );
        categoryRemover.remove(deleteCategory);

        // then - 캐시가 무효화되어 DB에서 최신 데이터 조회
        List<Category> updatedCategories = categoryFinder.findAll(refrigerator.getId());
        assertThat(updatedCategories)
            .extracting(Category::getName)
            .doesNotContain("삭제될 카테고리");
    }

    @Test
    void 서로_다른_냉장고의_카테고리_캐시는_독립적으로_동작한다() {
        // given
        Member member2 = memberRepository.save(member(fixtureMonkey).sample());
        Refrigerator refrigerator2 = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());

        MemberRefrigeratorKey key1 = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());
        MemberRefrigeratorKey key2 = new MemberRefrigeratorKey(member2.getId(), refrigerator2.getId());

        categoryAppender.appendDefaultCategories(key1);
        categoryAppender.appendDefaultCategories(key2);

        categoryFinder.findAll(refrigerator.getId());
        categoryFinder.findAll(refrigerator2.getId());

        Cache cache = cacheManager.getCache("categories");
        assertThat(cache.get(refrigerator.getId())).isNotNull();
        assertThat(cache.get(refrigerator2.getId())).isNotNull();

        // when
        AddCategory addCategory = new AddCategory("새 카테고리", refrigerator.getId(), member.getId());
        categoryAppender.appendCustomCategory(addCategory);

        // then
        assertThat(cache.get(refrigerator.getId())).isNull();
        assertThat(cache.get(refrigerator2.getId())).isNotNull();

        List<Category> refrigerator1Categories = categoryFinder.findAll(refrigerator.getId());
        List<Category> refrigerator2Categories = categoryFinder.findAll(refrigerator2.getId());

        assertThat(refrigerator1Categories).hasSize(9);
        assertThat(refrigerator2Categories).hasSize(8);
    }

    @Test
    void 캐시_키가_refrigeratorId로_정확하게_생성된다() {
        // given
        categoryAppender.appendDefaultCategories(
            new MemberRefrigeratorKey(member.getId(), refrigerator.getId())
        );

        // when
        categoryFinder.findAll(refrigerator.getId());

        // then
        Cache cache = cacheManager.getCache("categories");
        assertThat(cache).isNotNull();
        assertThat(cache.get(refrigerator.getId())).isNotNull();
        assertThat(cache.get(refrigerator.getId()).get()).isInstanceOf(List.class);
    }

    @Test
    void 캐시된_데이터와_DB_데이터가_일치한다() {
        // given
        categoryAppender.appendDefaultCategories(
            new MemberRefrigeratorKey(member.getId(), refrigerator.getId())
        );

        // when
        List<Category> cachedResult = categoryFinder.findAll(refrigerator.getId());

        // then
        List<Category> dbResult = categoryRepository.findAllByRefrigeratorIdAndStatus(
            refrigerator.getId(),
            soon.fridgely.domain.EntityStatus.ACTIVE
        );
        assertThat(cachedResult).hasSize(dbResult.size());
        assertThat(cachedResult)
            .extracting(Category::getName)
            .containsExactlyInAnyOrderElementsOf(
                dbResult.stream().map(Category::getName).toList()
            );
    }

    @Test
    void 캐시_무효화_후_재조회_시_최신_데이터를_가져온다() {
        // given
        categoryAppender.appendDefaultCategories(
            new MemberRefrigeratorKey(member.getId(), refrigerator.getId())
        );
        categoryFinder.findAll(refrigerator.getId());

        // when
        categoryAppender.appendCustomCategory(
            new AddCategory("테스트 카테고리", refrigerator.getId(), member.getId())
        );

        // then
        List<Category> firstResult = categoryFinder.findAll(refrigerator.getId());
        List<Category> secondResult = categoryFinder.findAll(refrigerator.getId());

        assertThat(firstResult).hasSize(9);
        assertThat(secondResult).hasSize(9);
        assertThat(firstResult).isEqualTo(secondResult);
    }

}