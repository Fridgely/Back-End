package soon.fridgely.domain.refrigerator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;

import java.util.Optional;

public interface RefrigeratorRepository extends JpaRepository<Refrigerator, Long> {

    Optional<Refrigerator> findByName(String name);

}