package soon.fridgely.domain.food.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodSortType;
import soon.fridgely.domain.food.entity.StorageType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static soon.fridgely.domain.category.entity.QCategory.category;
import static soon.fridgely.domain.food.entity.QFood.food;
import static soon.fridgely.domain.refrigerator.entity.QMemberRefrigerator.memberRefrigerator;

/**
 * FoodRepository QueryDSL 구현체
 */
@RequiredArgsConstructor
@Repository
public class FoodRepositoryImpl implements FoodRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 냉장고의 음식을 동적 조건과 정렬로 조회
     */
    @Override
    public Slice<Food> findAllDynamic(
        long refrigeratorId,
        long cursorId,
        FoodSortType sortType,
        StorageType storageType,
        Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder()
            .and(food.refrigerator.id.eq(refrigeratorId))
            .and(food.status.eq(EntityStatus.ACTIVE))
            .and(food.id.lt(cursorId));

        if (storageType != null) {
            builder.and(food.storageType.eq(storageType));
        }

        OrderSpecifier<?> primaryOrder = resolveSortOrder(sortType);

        List<Food> content = queryFactory
            .selectFrom(food)
            .join(food.category, category).fetchJoin()
            .where(builder)
            .orderBy(primaryOrder, food.id.desc())
            .limit(pageable.getPageSize() + 1)
            .fetch();

        return toSlice(content, pageable);
    }

    /**
     * 특정 회원이 소유한 모든 음식 조회 (유통기한 오름차순)
     * - MemberRefrigerator를 통해 회원이 참여한 냉장고의 음식만 조회
     */
    @Override
    public List<Food> findAllMyFoods(long memberId) {
        return queryFactory
            .selectFrom(food)
            .join(food.category, category).fetchJoin()
            .join(memberRefrigerator)
            .on(memberRefrigerator.refrigerator.eq(food.refrigerator))
            .where(
                memberRefrigerator.member.id.eq(memberId),
                memberRefrigerator.status.eq(EntityStatus.ACTIVE),
                food.status.eq(EntityStatus.ACTIVE)
            )
            .orderBy(food.expirationDate.asc(), food.id.desc())
            .fetch();
    }

    /**
     * 특정 회원의 음식 중 지정된 날짜 범위에 만료되는 음식 조회
     * - 알림 배치에서 활용
     */
    @Override
    public List<Food> findMyFoodsExpiringBetween(
        long memberId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    ) {
        return queryFactory
            .selectFrom(food)
            .join(memberRefrigerator)
            .on(memberRefrigerator.refrigerator.eq(food.refrigerator))
            .where(
                memberRefrigerator.member.id.eq(memberId),
                memberRefrigerator.status.eq(EntityStatus.ACTIVE),
                food.status.eq(EntityStatus.ACTIVE),
                food.expirationDate.between(startDateTime, endDateTime)
            )
            .orderBy(food.expirationDate.asc())
            .fetch();
    }

    /**
     * 특정 회원의 재고가 0인 음식 조회
     */
    @Override
    public List<Food> findAllOutOfStock(long memberId) {
        return queryFactory
            .selectFrom(food)
            .join(memberRefrigerator)
            .on(memberRefrigerator.refrigerator.eq(food.refrigerator))
            .where(
                memberRefrigerator.member.id.eq(memberId),
                memberRefrigerator.status.eq(EntityStatus.ACTIVE),
                food.status.eq(EntityStatus.ACTIVE),
                food.quantity.amount.eq(BigDecimal.ZERO)
            )
            .fetch();
    }

    /**
     * 특정 냉장고의 음식을 ID로 조회 (카테고리 Fetch Join)
     */
    @Override
    public Optional<Food> findByIdAndRefrigeratorIdWithCategory(long foodId, long refrigeratorId) {
        Food result = queryFactory
            .selectFrom(food)
            .join(food.category, category).fetchJoin()
            .where(
                food.id.eq(foodId),
                food.refrigerator.id.eq(refrigeratorId),
                food.status.eq(EntityStatus.ACTIVE)
            )
            .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * FoodSortType에 따른 정렬 조건 반환
     */
    private OrderSpecifier<?> resolveSortOrder(FoodSortType sortType) {
        return switch (sortType) {
            case EXPIRATION -> food.expirationDate.asc();
            case CREATED -> food.createdAt.desc();
            case NAME -> food.name.asc();
        };
    }

    /**
     * List를 Slice로 변환 (커서 기반 페이지네이션)
     * - limit + 1로 조회하여 hasNext 판단
     */
    private Slice<Food> toSlice(List<Food> content, Pageable pageable) {
        boolean hasNext = content.size() > pageable.getPageSize();

        List<Food> result = hasNext
            ? new ArrayList<>(content.subList(0, pageable.getPageSize()))
            : content;

        return new SliceImpl<>(result, pageable, hasNext);
    }

}