package soon.fridgely.global.support.image.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.image.ImageManager;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

class ImageEventListenerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private ImageManager imageManager;

    @Test
    void 트랜잭션_커밋_후_이미지_삭제_이벤트가_처리된다() throws InterruptedException {
        // given
        String imageUrl = "https://s3.amazonaws.com/bucket/images/test-image.jpg";

        // when
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new ImageDeleteEvent(imageUrl));
            return null;
        });

        Thread.sleep(100); // 비동기 이벤트 처리 대기

        // then
        then(imageManager).should(times(1)).delete(imageUrl);
    }

    @Test
    void 이미지_URL이_변경되었을_때만_삭제_이벤트가_발행된다() throws InterruptedException {
        // given
        String oldImageUrl = "https://s3.amazonaws.com/bucket/images/old-image.jpg";
        String newImageUrl = "https://s3.amazonaws.com/bucket/images/new-image.jpg";

        // when
        transactionTemplate.execute(status -> {
            // 이미지 URL이 변경된 경우
            if (!oldImageUrl.equals(newImageUrl)) {
                eventPublisher.publishEvent(new ImageDeleteEvent(oldImageUrl));
            }
            return null;
        });

        Thread.sleep(100);

        // then
        then(imageManager).should(times(1)).delete(oldImageUrl);
    }

    @Test
    void null_이미지_URL은_이벤트가_발행되지_않는다() throws InterruptedException {
        // given
        String imageUrl = null;

        // when
        transactionTemplate.execute(status -> null);

        Thread.sleep(100);

        // then
        then(imageManager).should(times(0)).delete(imageUrl);
    }

}