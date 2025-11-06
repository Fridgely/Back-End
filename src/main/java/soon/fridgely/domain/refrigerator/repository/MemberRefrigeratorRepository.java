package soon.fridgely.domain.refrigerator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;

import java.util.Optional;

public interface MemberRefrigeratorRepository extends JpaRepository<MemberRefrigerator, Long> {

    Optional<MemberRefrigerator> findByMemberAndRefrigerator(Member member, Refrigerator refrigerator);

}