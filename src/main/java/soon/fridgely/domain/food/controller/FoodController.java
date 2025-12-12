package soon.fridgely.domain.food.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.dto.request.FoodStockUpdateRequest;
import soon.fridgely.domain.food.dto.request.FoodUpdateRequest;
import soon.fridgely.domain.food.dto.response.FoodDetailResponse;
import soon.fridgely.domain.food.dto.response.FoodResponse;
import soon.fridgely.domain.food.service.FoodService;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.global.security.annotation.LoginMember;
import soon.fridgely.global.support.CursorPageRequest;
import soon.fridgely.global.support.response.ApiResponse;

@RequiredArgsConstructor
@RequestMapping("/api/v1/refrigerators/{refrigeratorId}/foods")
@RestController
public class FoodController {

    private final FoodService foodService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> createFood(
        @RequestPart(value = "request") @Valid FoodCreateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile file,
        @LoginMember Long memberId,
        @PathVariable long refrigeratorId
    ) {
        foodService.createFood(request, file, new MemberRefrigeratorKey(memberId, refrigeratorId));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    @PatchMapping(value = "/{foodId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> updateFood(
        @RequestPart(value = "request") @Valid FoodUpdateRequest request,
        @RequestPart(value = "image", required = false) MultipartFile image,
        @LoginMember Long memberId,
        @PathVariable long refrigeratorId,
        @PathVariable long foodId
    ) {
        foodService.updateFood(foodId, request, image, new MemberRefrigeratorKey(memberId, refrigeratorId));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PatchMapping("/{foodId}/stock")
    public ResponseEntity<ApiResponse<?>> updateFoodStock(
        @RequestBody @Valid FoodStockUpdateRequest request,
        @LoginMember Long memberId,
        @PathVariable long refrigeratorId,
        @PathVariable long foodId
    ) {
        foodService.updateFoodStock(foodId, request, new MemberRefrigeratorKey(memberId, refrigeratorId));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{foodId}")
    public ResponseEntity<ApiResponse<FoodDetailResponse>> findFood(
        @LoginMember Long memberId,
        @PathVariable long refrigeratorId,
        @PathVariable long foodId
    ) {
        FoodDetailResponse response = foodService.findFood(foodId, new MemberRefrigeratorKey(memberId, refrigeratorId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Slice<FoodResponse>>> findAllFoods(
        @LoginMember Long memberId,
        CursorPageRequest cursorRequest,
        @PathVariable long refrigeratorId
    ) {
        Slice<FoodResponse> responses = foodService.findAllFoods(new MemberRefrigeratorKey(memberId, refrigeratorId), cursorRequest);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @DeleteMapping("/{foodId}")
    public ResponseEntity<ApiResponse<?>> deleteFood(
        @LoginMember Long memberId,
        @PathVariable long refrigeratorId,
        @PathVariable long foodId
    ) {
        foodService.deleteFood(foodId, new MemberRefrigeratorKey(memberId, refrigeratorId));
        return ResponseEntity.ok(ApiResponse.success());
    }

}