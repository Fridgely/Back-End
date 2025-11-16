package soon.fridgely.domain.food.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import soon.fridgely.domain.food.dto.request.FoodCreateRequest;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.validator.RefrigeratorAccessValidator;
import soon.fridgely.global.support.image.ImageManager;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
public class FoodService {

    private final FoodManager foodManager;
    private final ImageManager imageManager;

    private final RefrigeratorAccessValidator refrigeratorAccessValidator;

    public void createFood(FoodCreateRequest request, MultipartFile file, MemberRefrigeratorKey key) {
        refrigeratorAccessValidator.validateMembership(key.refrigeratorId(), key.memberId());

        String uploadedUrl = imageManager.upload(file);
        foodManager.createFood(request.toFoodInfo(LocalDate.now(), uploadedUrl), key);
    }

}
