package soon.fridgely.domain.food.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import soon.fridgely.domain.food.dto.response.FoodStatusResponse;
import soon.fridgely.domain.food.service.FoodService;
import soon.fridgely.global.security.annotation.LoginMember;
import soon.fridgely.global.support.response.ApiResponse;

/**
 * 해당 컨트롤러는 현재 로그인한 사용자를 기준으로 냉장고에 구애받지 않음
 */
@RequiredArgsConstructor
@RequestMapping("/api/v1/foods")
@RestController
public class MyFoodController {

    private final FoodService foodService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<FoodStatusResponse>> findAllMyFoodsGroupedByStatus(
        @LoginMember Long memberId
    ) {
        FoodStatusResponse response = foodService.findAllMyFoodsGroupedByStatus(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}