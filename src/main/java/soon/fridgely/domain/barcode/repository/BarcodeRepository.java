package soon.fridgely.domain.barcode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.barcode.entity.Barcode;

public interface BarcodeRepository extends JpaRepository<Barcode, Long> {
}