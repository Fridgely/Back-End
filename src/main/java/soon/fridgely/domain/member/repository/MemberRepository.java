package soon.fridgely.domain.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.member.entity.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByLoginId(String loginId);

}