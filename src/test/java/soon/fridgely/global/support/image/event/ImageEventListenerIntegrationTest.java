package soon.fridgely.global.support.image.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.image.ImageManager;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

class ImageEventListenerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private ImageManager imageManager;

    @Test
    void 트랜잭션_커밋_후_이미지_삭제_이벤트가_처리된다() {
        // given
        String imageUrl = "https://s3.amazonaws.com/bucket/images/test-image.jpg";

        // when
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new ImageDeleteEvent(imageUrl));
            return null;
        });

        // then
        then(imageManager).should(times(1)).delete(imageUrl);
    }

    @Test
    void 이미지_삭제_실패_시_예외가_전파되지_않는다() {
        // given
        String imageUrl = "https://s3.amazonaws.com/bucket/images/old-image.jpg";
        willThrow(new RuntimeException("S3 삭제 실패"))
            .given(imageManager).delete(imageUrl);

        // when - 예외가 발생해도 트랜잭션은 정상 처리됨
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new ImageDeleteEvent(imageUrl));
            return null;
        });

        // then - imageManager.delete가 호출되었지만 예외는 전파되지 않음
        then(imageManager).should(times(1)).delete(imageUrl);
    }

    @Test
    void null_이미지_URL_이벤트가_발행되면_삭제가_호출되지_않는다() {
        // given
        String imageUrl = null;

        // when
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new ImageDeleteEvent(imageUrl));
            return null;
        });

        // then
        then(imageManager).should(times(0)).delete(imageUrl);
    }

}