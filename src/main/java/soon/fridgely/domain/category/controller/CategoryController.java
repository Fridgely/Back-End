package soon.fridgely.domain.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import soon.fridgely.domain.category.controller.dto.request.CategoryAddRequest;
import soon.fridgely.domain.category.controller.dto.request.CategoryModifyRequest;
import soon.fridgely.domain.category.dto.DeleteCategory;
import soon.fridgely.domain.category.service.CategoryService;
import soon.fridgely.global.support.annotation.LoginMember;
import soon.fridgely.global.support.response.ApiResponse;

@RequiredArgsConstructor
@RequestMapping("/api/v1/refrigerators")
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/{refrigeratorId}/categories")
    public ResponseEntity<ApiResponse<?>> append(
        @RequestBody @Valid CategoryAddRequest request,
        @LoginMember Long memberId,
        @PathVariable long refrigeratorId
    ) {
        categoryService.appendCustomCategory(request.toAddCategory(refrigeratorId, memberId));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    @PatchMapping("/{refrigeratorId}/categories/{categoryId}")
    public ResponseEntity<ApiResponse<?>> modify(
        @RequestBody @Valid CategoryModifyRequest request,
        @LoginMember Long memberId,
        @PathVariable long refrigeratorId,
        @PathVariable long categoryId
    ) {
        categoryService.modifyCustomCategory(request.toModifyCategory(refrigeratorId, categoryId, memberId));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{refrigeratorId}/categories/{categoryId}")
    public ResponseEntity<ApiResponse<?>> remove(
        @LoginMember Long memberId,
        @PathVariable long refrigeratorId,
        @PathVariable long categoryId
    ) {
        categoryService.removeCustomCategory(new DeleteCategory(memberId, refrigeratorId, categoryId));
        return ResponseEntity.ok(ApiResponse.success()); // 일관성을 위해 OK 반환
    }

}