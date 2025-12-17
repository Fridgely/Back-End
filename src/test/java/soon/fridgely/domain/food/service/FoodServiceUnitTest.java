package soon.fridgely.domain.food.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.dto.request.FoodStockUpdateRequest;
import soon.fridgely.domain.food.dto.request.FoodUpdateRequest;
import soon.fridgely.domain.food.dto.response.FoodResponse;
import soon.fridgely.domain.food.dto.response.FoodStatusResponse;
import soon.fridgely.domain.food.entity.*;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.global.support.CursorPageRequest;
import soon.fridgely.global.support.image.ImageManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FoodServiceUnitTest {

    @InjectMocks
    private FoodService foodService;

    @Mock
    private FoodFinder foodFinder;

    @Mock
    private FoodModifier foodModifier;

    @Mock
    private FoodManager foodManager;

    @Mock
    private ImageManager imageManager;

    @Test
    void 음식을_등록한다() {
        // given
        var request = new FoodCreateRequest(
            "foodName",
            1L,
            BigDecimal.ONE,
            Unit.KG,
            LocalDateTime.now().plusDays(2L),
            StorageType.FROZEN,
            "description"
        );

        MockMultipartFile mockFile = createMockFile();
        var key = new MemberRefrigeratorKey(1L, 1L);

        String expectedImageUrl = "http://s3.example.com/image.jpg";
        given(imageManager.upload(mockFile)).willReturn(expectedImageUrl);

        // when
        foodService.createFood(request, mockFile, key);

        // then
        InOrder inOrder = inOrder(imageManager, foodManager);
        then(imageManager).should(inOrder)
            .upload(mockFile);

        ArgumentCaptor<FoodInfo> foodInfoCaptor = ArgumentCaptor.forClass(FoodInfo.class);
        then(foodManager).should(inOrder)
            .createFood(foodInfoCaptor.capture(), eq(key), eq(request.categoryId()));

        FoodInfo foodInfo = foodInfoCaptor.getValue();
        assertThat(foodInfo).isNotNull()
            .extracting("name", "imageURL")
            .containsExactlyInAnyOrder(request.name(), expectedImageUrl);
    }

    @Test
    void 이미지가_포함된_음식을_수정한다() {
        // given
        long foodId = 1L;
        var key = new MemberRefrigeratorKey(1L, 1L);

        var request = new FoodUpdateRequest(
            "수정된 이름",
            2L,
            BigDecimal.TEN,
            Unit.KG,
            LocalDateTime.now().plusDays(10),
            StorageType.ROOM_TEMPERATURE,
            "수정된 설명"
        );

        MockMultipartFile mockFile = createMockFile();
        String expectedImageUrl = "http://s3.example.com/new-image.jpg";
        given(imageManager.upload(mockFile)).willReturn(expectedImageUrl);

        // when
        foodService.updateFood(foodId, request, mockFile, key);

        // then
        InOrder inOrder = inOrder(imageManager, foodModifier);
        then(imageManager).should(inOrder)
            .upload(mockFile);

        ArgumentCaptor<FoodInfo> foodInfoCaptor = ArgumentCaptor.forClass(FoodInfo.class);
        then(foodModifier).should(inOrder)
            .update(
                eq(foodId),
                foodInfoCaptor.capture(),
                eq(key),
                eq(request.categoryId())
            );

        FoodInfo capturedInfo = foodInfoCaptor.getValue();
        assertThat(capturedInfo).isNotNull()
            .extracting("name", "imageURL")
            .containsExactly(request.name(), expectedImageUrl);
    }

    @Test
    void 이미지가_포함되지_않은_경우_기존_이미지가_유지된다() {
        // given
        long foodId = 1L;
        var key = new MemberRefrigeratorKey(1L, 1L);

        var request = new FoodUpdateRequest(
            "수정된 이름",
            1L,
            BigDecimal.ONE,
            Unit.KG,
            LocalDateTime.now().plusDays(5),
            StorageType.FROZEN,
            "설명"
        );
        MultipartFile emptyFile = null;

        // when
        foodService.updateFood(foodId, request, emptyFile, key);

        // then
        then(imageManager).shouldHaveNoInteractions();

        ArgumentCaptor<FoodInfo> foodInfoCaptor = ArgumentCaptor.forClass(FoodInfo.class);
        then(foodModifier).should()
            .update(
                eq(foodId),
                foodInfoCaptor.capture(),
                eq(key),
                eq(request.categoryId())
            );

        FoodInfo capturedInfo = foodInfoCaptor.getValue();
        assertThat(capturedInfo.imageURL()).isNull();
        assertThat(capturedInfo.name()).isEqualTo(request.name());
    }

    @Test
    void 음식을_조회한다() {
        // given
        long foodId = 1L;
        var key = new MemberRefrigeratorKey(1L, 1L);

        Food mockFood = createMockFood(foodId, FoodStatus.BLACK);
        given(foodFinder.find(foodId, key.refrigeratorId())).willReturn(mockFood);

        // when
        var response = foodService.findFood(foodId, key);

        // then
        InOrder inOrder = inOrder(foodFinder);
        then(foodFinder).should(inOrder)
            .find(foodId, key.refrigeratorId());

        assertThat(response).isNotNull()
            .extracting("id", "name", "categoryName")
            .containsExactly(
                mockFood.getId(),
                mockFood.getName(),
                mockFood.getCategory().getName()
            );
    }

    @Test
    void 음식_목록을_조회한다() {
        // given
        var key = new MemberRefrigeratorKey(1L, 1L);
        var request = new CursorPageRequest(null, 10);

        long expectedCursorId = request.getCursorId(); // Long.MAX_VALUE
        Pageable expectedPageable = request.toPageable();

        Food mockFood = createMockFood(1L, FoodStatus.BLACK);
        var foodList = List.of(mockFood);

        Slice<Food> mockFoodSlice = new SliceImpl<>(foodList, expectedPageable, true);

        given(foodFinder.findAll(key.refrigeratorId(), expectedCursorId, expectedPageable))
            .willReturn(mockFoodSlice);

        // when
        Slice<FoodResponse> responseSlice = foodService.findAllFoods(key, request);

        // then
        InOrder inOrder = inOrder(foodFinder);
        then(foodFinder).should(inOrder)
            .findAll(key.refrigeratorId(), expectedCursorId, expectedPageable);

        assertThat(responseSlice).isNotNull();
        assertThat(responseSlice.hasNext()).isTrue();
        assertThat(responseSlice.getContent()).hasSize(1);

        assertThat(responseSlice.getContent().get(0))
            .extracting("id", "name", "imageURL")
            .containsExactly(
                mockFood.getId(),
                mockFood.getName(),
                mockFood.getImageURL()
            );
    }

    @Test
    void 음식을_삭제한다() {
        // given
        long foodId = 1L;
        var key = new MemberRefrigeratorKey(1L, 1L);

        // when
        foodService.deleteFood(foodId, key);

        // then
        InOrder inOrder = inOrder(foodManager);
        then(foodManager).should(inOrder)
            .delete(foodId, key.refrigeratorId());
    }

    @Test
    void 사용자의_모든_음식을_조회하여_상태별로_그룹핑_한다() {
        // given
        long memberId = 1L;

        Food redFood = createMockFood(1L, FoodStatus.RED);
        Food greenFood1 = createMockFood(2L, FoodStatus.GREEN);
        Food greenFood2 = createMockFood(3L, FoodStatus.GREEN);
        Food blackFood = createMockFood(4L, FoodStatus.BLACK);

        List<Food> allFoods = List.of(redFood, greenFood1, greenFood2, blackFood);
        given(foodFinder.findAllMyFoods(memberId)).willReturn(allFoods);

        // when
        FoodStatusResponse response = foodService.findAllMyFoodsGroupedByStatus(memberId);

        // then
        assertThat(response.red()).hasSize(1);
        assertThat(response.green()).hasSize(2);
        assertThat(response.black()).hasSize(1);
        assertThat(response.yellow()).isNotNull()
            .isEmpty();
    }

    @Test
    void 음식의_재고를_추가한다() {
        // given
        long foodId = 1L;
        var request = new FoodStockUpdateRequest(BigDecimal.ONE, Unit.KG, StockActionType.ADD);
        var key = new MemberRefrigeratorKey(1L, 1L);

        // when
        foodService.updateFoodStock(foodId, request, key);

        // then
        then(foodModifier).should()
            .add(foodId, key.refrigeratorId(), request.toQuantity());
    }

    @Test
    void 음식을_소비한다() {
        // given
        long foodId = 1L;
        var request = new FoodStockUpdateRequest(BigDecimal.ONE, Unit.KG, StockActionType.CONSUME);
        var key = new MemberRefrigeratorKey(1L, 1L);
        Quantity amount = request.toQuantity();

        // when
        foodService.updateFoodStock(foodId, request, key);

        // then
        then(foodModifier).should()
            .consume(foodId, key.refrigeratorId(), amount);
    }

    private MockMultipartFile createMockFile() {
        byte[] content = new byte[1024];
        return new MockMultipartFile("image", "originalFilename.jpg", "image/jpeg", content);
    }

    private Food createMockFood(long foodId, FoodStatus status) {
        Food mockFood = mock(Food.class);
        Category mockCategory = mock(Category.class);
        Quantity mockQuantity = mock(Quantity.class);

        given(mockFood.getId()).willReturn(foodId);
        given(mockFood.getName()).willReturn("TestFood");
        given(mockFood.getImageURL()).willReturn("http://example.com/test.jpg");
        given(mockFood.getFoodStatus()).willReturn(status);
        given(mockFood.getCategory()).willReturn(mockCategory);
        given(mockCategory.getName()).willReturn("TestCategory");
        given(mockFood.getQuantity()).willReturn(mockQuantity);
        given(mockQuantity.amount()).willReturn(BigDecimal.TEN);
        given(mockQuantity.unit()).willReturn(Unit.PIECE);

        return mockFood;
    }

}