package soon.fridgely.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.member.entity.MemberDevice;

import java.util.List;

public interface MemberDeviceRepository extends JpaRepository<MemberDevice, Long> {

    List<MemberDevice> findAllByMemberId(long memberId);

}