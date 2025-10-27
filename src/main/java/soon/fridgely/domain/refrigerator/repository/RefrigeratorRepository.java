package soon.fridgely.domain.refrigerator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;

public interface RefrigeratorRepository extends JpaRepository<Refrigerator, Long> {
}