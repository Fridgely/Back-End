package soon.fridgely.domain.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.category.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}