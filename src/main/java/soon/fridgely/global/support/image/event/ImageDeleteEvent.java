package soon.fridgely.global.support.image.event;

/**
 * 이미지 삭제 이벤트
 * 트랜잭션 커밋 후 이미지 삭제를 지연 실행하기 위해 사용
 */
public record ImageDeleteEvent(String imageUrl) {
}