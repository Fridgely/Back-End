package soon.fridgely.domain.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByRefrigeratorAndStatus(Refrigerator refrigerator, EntityStatus status);

    boolean existsByNameAndRefrigeratorAndStatus(String name, Refrigerator refrigerator, EntityStatus status);

}