package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.domain.food.repository.FoodRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Component
public class FoodStatusProcessor {

    private final FoodRepository foodRepository;

    /**
     * 유통기한 기반 FoodStatus 일괄 갱신
     */
    @Transactional
    public int bulkUpdate(LocalDate today) {
        LocalDateTime redStart = today.atStartOfDay();
        LocalDateTime yellowStart = today.plusDays(FoodStatus.RED.nextThresholdDay()).atStartOfDay();
        LocalDateTime greenStart = today.plusDays(FoodStatus.YELLOW.nextThresholdDay()).atStartOfDay();

        int totalUpdated = 0;
        totalUpdated += foodRepository.bulkUpdateFoodStatus(FoodStatus.BLACK, null, redStart);
        totalUpdated += foodRepository.bulkUpdateFoodStatus(FoodStatus.RED, redStart, yellowStart);
        totalUpdated += foodRepository.bulkUpdateFoodStatus(FoodStatus.YELLOW, yellowStart, greenStart);
        totalUpdated += foodRepository.bulkUpdateFoodStatus(FoodStatus.GREEN, greenStart, null);

        log.debug("[FoodStatusProcessor] FoodStatus 갱신 완료. (기준일={}, 변경건수={})", today, totalUpdated);

        return totalUpdated;
    }

}