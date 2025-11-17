package soon.fridgely.domain.food.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.entity.*;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.validator.RefrigeratorAccessValidator;
import soon.fridgely.global.support.image.ImageManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private FoodManager foodManager;

    @Mock
    private ImageManager imageManager;

    @Mock
    private RefrigeratorAccessValidator refrigeratorAccessValidator;

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
        InOrder inOrder = inOrder(refrigeratorAccessValidator, imageManager, foodManager);

        then(refrigeratorAccessValidator).should(inOrder)
            .validateMembership(key);

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
    void 음식을_조회한다() {
        // given
        long foodId = 1L;
        var key = new MemberRefrigeratorKey(1L, 1L);

        Food mockFood = createMockFood(foodId);
        given(foodManager.find(foodId, key.refrigeratorId())).willReturn(mockFood);

        // when
        var response = foodService.findFood(foodId, key);

        // then
        InOrder inOrder = inOrder(refrigeratorAccessValidator, foodManager);
        then(refrigeratorAccessValidator).should(inOrder)
            .validateMembership(key);
        then(foodManager).should(inOrder)
            .find(foodId, key.refrigeratorId());

        assertThat(response).isNotNull()
            .extracting("id", "name", "categoryName")
            .containsExactly(
                mockFood.getId(),
                mockFood.getName(),
                mockFood.getCategory().getName()
            );
    }

    private MockMultipartFile createMockFile() {
        byte[] content = new byte[1024];
        return new MockMultipartFile("image", "originalFilename.jpg", "image/jpeg", content);
    }

    private Food createMockFood(long foodId) {
        Food mockFood = mock(Food.class);
        Category mockCategory = mock(Category.class);
        Quantity mockQuantity = mock(Quantity.class);
        LocalDateTime testDate = LocalDateTime.now().plusDays(5);

        given(mockFood.getId()).willReturn(foodId);
        given(mockFood.getName()).willReturn("TestFood");
        given(mockFood.getQuantity()).willReturn(mockQuantity);
        given(mockQuantity.amount()).willReturn(BigDecimal.TEN);
        given(mockQuantity.unit()).willReturn(Unit.PIECE);
        given(mockFood.getExpirationDate()).willReturn(testDate);
        given(mockFood.getStorageType()).willReturn(StorageType.REFRIGERATION);
        given(mockFood.getFoodStatus()).willReturn(FoodStatus.GREEN);
        given(mockFood.getDescription()).willReturn("TestDescription");
        given(mockFood.getImageURL()).willReturn("http://example.com/test.jpg");

        given(mockFood.getCategory()).willReturn(mockCategory);
        given(mockCategory.getName()).willReturn("TestCategory");

        return mockFood;
    }

}