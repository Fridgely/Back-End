package soon.fridgely.domain.food.service;

import soon.fridgely.domain.food.entity.Food;

/**
 * 식재료 재고 관련 이벤트 처리를 담당하는 핸들러
 * ex) 알림 발송, 장바구니 자동 추가 등
 */
public interface FoodStockHandler {

    /**
     * 식재료 재고가 소진되었을 때 처리하는 메서드
     *
     * @param food 재고가 소진된 식재료
     */
    void onStockExhausted(Food food);

}