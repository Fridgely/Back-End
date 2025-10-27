package soon.fridgely.domain.refrigerator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;

public interface MemberRefrigeratorRepository extends JpaRepository<MemberRefrigerator, Long> {
}