package soon.fridgely.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByLoginId(String loginId);

}