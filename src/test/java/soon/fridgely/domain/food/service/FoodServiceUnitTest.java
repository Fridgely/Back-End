package soon.fridgely.domain.food.service;

import com.navercorp.fixturemonkey.FixtureMonkey;
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
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.dto.request.FoodCursorPageRequest;
import soon.fridgely.domain.food.dto.request.FoodUpdateRequest;
import soon.fridgely.domain.food.dto.response.FoodListResponse;
import soon.fridgely.domain.food.dto.response.FoodStatusResponse;
import soon.fridgely.domain.food.entity.Food;
import soon.fridgely.domain.food.entity.FoodSortType;
import soon.fridgely.domain.food.entity.FoodStatus;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.global.support.FixtureMonkeyFactory;
import soon.fridgely.global.support.image.ImageManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.inOrder;

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

    private final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    @Test
    void 음식을_등록한다() {
        // given
        var request = fixtureMonkey.giveMeOne(FoodCreateRequest.class);
        var key = fixtureMonkey.giveMeOne(MemberRefrigeratorKey.class);
        MockMultipartFile mockFile = createMockFile();

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
        var request = fixtureMonkey.giveMeOne(FoodUpdateRequest.class);
        var key = fixtureMonkey.giveMeOne(MemberRefrigeratorKey.class);
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
        var request = fixtureMonkey.giveMeOne(FoodUpdateRequest.class);
        var key = fixtureMonkey.giveMeOne(MemberRefrigeratorKey.class);
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
        var key = fixtureMonkey.giveMeOne(MemberRefrigeratorKey.class);
        Food mockFood = fixtureMonkey.giveMeOne(Food.class);
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
        var key = fixtureMonkey.giveMeOne(MemberRefrigeratorKey.class);
        var request = new FoodCursorPageRequest(null, 10, null, null);
        long expectedCursorId = request.getCursorId();
        Pageable expectedPageable = request.toPageable();
        FoodSortType expectedSortBy = request.getSortBy();

        List<Food> foods = fixtureMonkey.giveMe(Food.class, 2);
        Slice<Food> foodSlice = new SliceImpl<>(foods, expectedPageable, true);

        given(foodFinder.findAll(key.refrigeratorId(), expectedCursorId, expectedPageable, expectedSortBy, null))
            .willReturn(foodSlice);

        // when
        Slice<FoodListResponse> responseSlice = foodService.findAllFoods(key, request);

        // then
        InOrder inOrder = inOrder(foodFinder);
        then(foodFinder).should(inOrder)
            .findAll(key.refrigeratorId(), expectedCursorId, expectedPageable, expectedSortBy, null);

        assertThat(responseSlice).isNotNull();
        assertThat(responseSlice.hasNext()).isTrue();
        assertThat(responseSlice.getContent()).hasSize(2);

        assertThat(responseSlice.getContent()
            .get(0)
            .id()
        ).isEqualTo(foods.get(0).getId());
    }

    @Test
    void 사용자의_모든_음식을_조회하여_상태별로_그룹핑_한다() {
        // given
        long memberId = 1L;

        Food redFood = fixtureMonkey.giveMeBuilder(Food.class)
            .set("foodStatus", FoodStatus.RED)
            .sample();
        Food greenFood1 = fixtureMonkey.giveMeBuilder(Food.class)
            .set("foodStatus", FoodStatus.GREEN)
            .sample();
        Food greenFood2 = fixtureMonkey.giveMeBuilder(Food.class)
            .set("foodStatus", FoodStatus.GREEN)
            .sample();
        Food blackFood = fixtureMonkey.giveMeBuilder(Food.class)
            .set("foodStatus", FoodStatus.BLACK)
            .sample();

        List<Food> allFoods = List.of(redFood, greenFood1, greenFood2, blackFood);
        given(foodFinder.findAllMyFoods(memberId)).willReturn(allFoods);

        // when
        FoodStatusResponse response = foodService.findAllMyFoodsGroupedByStatus(memberId);

        // then
        assertThat(response.red()).hasSize(1);
        assertThat(response.redCount()).isEqualTo(1);

        assertThat(response.green()).hasSize(2);
        assertThat(response.greenCount()).isEqualTo(2);

        assertThat(response.black()).hasSize(1);
        assertThat(response.blackCount()).isEqualTo(1);

        assertThat(response.yellow()).isNotNull()
            .isEmpty();
        assertThat(response.yellowCount()).isZero();
    }

    @Test
    void 저장_위치로_필터링하여_음식_목록을_조회한다() {
        // given
        var key = fixtureMonkey.giveMeOne(MemberRefrigeratorKey.class);
        var storageType = StorageType.REFRIGERATION;
        var request = fixtureMonkey.giveMeBuilder(FoodCursorPageRequest.class)
            .set("size", 10)
            .set("storageType", storageType)
            .sample();

        List<Food> foods = fixtureMonkey.giveMe(Food.class, 2);
        Slice<Food> foodSlice = new SliceImpl<>(foods, request.toPageable(), true);

        given(foodFinder.findAll(key.refrigeratorId(), request.getCursorId(), request.toPageable(), request.getSortBy(), storageType))
            .willReturn(foodSlice);

        // when
        Slice<FoodListResponse> responseSlice = foodService.findAllFoods(key, request);

        // then
        InOrder inOrder = inOrder(foodFinder);
        then(foodFinder).should(inOrder)
            .findAll(key.refrigeratorId(), request.getCursorId(), request.toPageable(), request.getSortBy(), storageType);

        assertThat(responseSlice).isNotNull();
        assertThat(responseSlice.hasNext()).isTrue();
    }

    @Test
    void 음식_등록_중_DB_저장_실패_시_업로드된_이미지를_삭제한다() {
        // given
        var request = fixtureMonkey.giveMeOne(FoodCreateRequest.class);
        var key = fixtureMonkey.giveMeOne(MemberRefrigeratorKey.class);
        MockMultipartFile mockFile = createMockFile();
        String uploadedUrl = "http://s3.example.com/image.jpg";

        given(imageManager.upload(mockFile)).willReturn(uploadedUrl);
        willThrow(new RuntimeException("DB 오류"))
            .given(foodManager).createFood(any(), any(), anyLong());

        // expected
        assertThatThrownBy(() -> foodService.createFood(request, mockFile, key))
            .isInstanceOf(RuntimeException.class);

        then(imageManager).should().delete(uploadedUrl);
    }

    @Test
    void 이미지가_있는_음식_수정_중_저장_실패_시_업로드된_이미지를_삭제한다() {
        // given
        long foodId = 1L;
        var request = fixtureMonkey.giveMeOne(FoodUpdateRequest.class);
        var key = fixtureMonkey.giveMeOne(MemberRefrigeratorKey.class);
        MockMultipartFile mockFile = createMockFile();
        String uploadedUrl = "http://s3.example.com/new-image.jpg";

        given(imageManager.upload(mockFile)).willReturn(uploadedUrl);
        willThrow(new RuntimeException("DB 오류"))
            .given(foodModifier).update(anyLong(), any(), any(), anyLong());

        // expected
        assertThatThrownBy(() -> foodService.updateFood(foodId, request, mockFile, key))
            .isInstanceOf(RuntimeException.class);

        then(imageManager).should().delete(uploadedUrl);
    }

    @Test
    void 이미지가_없는_음식_수정_중_저장_실패_시_이미지_삭제는_호출되지_않는다() {
        // given
        long foodId = 1L;
        var request = fixtureMonkey.giveMeOne(FoodUpdateRequest.class);
        var key = fixtureMonkey.giveMeOne(MemberRefrigeratorKey.class);

        willThrow(new RuntimeException("DB 오류"))
            .given(foodModifier).update(anyLong(), any(), any(), anyLong());

        // expected
        assertThatThrownBy(() -> foodService.updateFood(foodId, request, null, key))
            .isInstanceOf(RuntimeException.class);

        then(imageManager).shouldHaveNoInteractions();
    }

    private MockMultipartFile createMockFile() {
        byte[] content = new byte[1024];
        return new MockMultipartFile("image", "originalFilename.jpg", "image/jpeg", content);
    }

}