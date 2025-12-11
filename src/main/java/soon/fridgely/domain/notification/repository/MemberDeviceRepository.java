package soon.fridgely.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.MemberDevice;

import java.util.List;
import java.util.Optional;

public interface MemberDeviceRepository extends JpaRepository<MemberDevice, Long> {

    List<MemberDevice> findAllByMemberId(long memberId);

    Optional<MemberDevice> findByMemberIdAndTokenAndStatus(long memberId, String token, EntityStatus status);

}