package soon.fridgely.domain.barcode.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import soon.fridgely.domain.barcode.entity.BarcodeInput;

public interface BarcodeInputRepository extends JpaRepository<BarcodeInput, Long> {
}