package soon.fridgely.domain.notification.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.MemberDevice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberDeviceRepository extends JpaRepository<MemberDevice, Long> {

    List<MemberDevice> findAllByMemberId(long memberId);

    Optional<MemberDevice> findByMemberIdAndTokenAndStatus(long memberId, String token, EntityStatus status);

    /**
     * 특정 상태이며, 마지막 사용일이 임계값 이전인 디바이스들을 커서 기반 조회
     */
    @Query("""
        SELECT md FROM MemberDevice md
        WHERE md.status = :status
          AND md.lastUsedAt < :threshold
          AND md.id < :cursorId
        ORDER BY md.id DESC
        """)
    Slice<MemberDevice> findInactiveDevices(
        @Param("status") EntityStatus status,
        @Param("threshold") LocalDateTime threshold,
        @Param("cursorId") long cursorId,
        Pageable pageable
    );

    /**
     * 여러 디바이스를 한 번의 쿼리로 삭제 상태로 변경
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberDevice md SET md.status = 'DELETED' WHERE md.id IN :ids")
    int bulkUpdateStatusToDeleted(@Param("ids") List<Long> ids);

}