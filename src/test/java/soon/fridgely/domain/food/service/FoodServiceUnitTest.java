package soon.fridgely.domain.food.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.food.dto.command.FoodInfo;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.food.entity.Unit;
import soon.fridgely.domain.refrigerator.dto.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.validator.RefrigeratorAccessValidator;
import soon.fridgely.global.support.image.ImageManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

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
            .validateMembership(key.refrigeratorId(), key.memberId());

        then(imageManager).should(inOrder)
            .upload(mockFile);

        ArgumentCaptor<FoodInfo> foodInfoCaptor = ArgumentCaptor.forClass(FoodInfo.class);
        then(foodManager).should(inOrder)
            .createFood(foodInfoCaptor.capture(), eq(key));

        FoodInfo foodInfo = foodInfoCaptor.getValue();
        assertThat(foodInfo).isNotNull()
            .extracting("name", "imageURL")
            .containsExactlyInAnyOrder(request.name(), expectedImageUrl);
    }

    private MockMultipartFile createMockFile() {
        byte[] content = new byte[1024];
        return new MockMultipartFile("image", "originalFilename.jpg", "image/jpeg", content);
    }

}