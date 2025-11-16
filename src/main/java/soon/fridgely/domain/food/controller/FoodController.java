package soon.fridgely.domain.food.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.service.FoodService;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.global.support.annotation.LoginMember;
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

}