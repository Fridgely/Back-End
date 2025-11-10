package soon.fridgely.domain.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndRefrigeratorIdAndStatus(Long id, Long refrigeratorId, EntityStatus status);

    Optional<Category> findByNameAndRefrigeratorIdAndStatus(String name, Long refrigeratorId, EntityStatus status);

    List<Category> findAllByRefrigeratorAndStatus(Refrigerator refrigerator, EntityStatus status);

    List<Category> findAllByRefrigeratorIdAndStatus(long refrigeratorId, EntityStatus entityStatus);

    boolean existsByNameAndRefrigeratorAndStatus(String name, Refrigerator refrigerator, EntityStatus status);

    boolean existsByRefrigeratorAndStatus(Refrigerator refrigerator, EntityStatus status);

}