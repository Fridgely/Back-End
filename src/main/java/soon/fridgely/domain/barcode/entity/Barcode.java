package soon.fridgely.domain.barcode.entity;

import jakarta.persistence.*;
import lombok.*;
import soon.fridgely.domain.BaseEntity;
import soon.fridgely.domain.food.entity.StorageType;

import static java.util.Objects.requireNonNull;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "barcodes", uniqueConstraints = @UniqueConstraint(name = "uk_barcodes_code", columnNames = {"code"}))
@Entity
public class Barcode extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StorageType storageType;

    @Column(nullable = false)
    private int defaultExpirationDays;

    public static Barcode register(
        String code,
        String name,
        String category,
        StorageType storageType,
        int defaultExpirationDays
    ) {
        return Barcode.builder()
            .code(requireNonNull(code, "code는 필수입니다."))
            .name(requireNonNull(name, "name는 필수입니다."))
            .category(requireNonNull(category, "category는 필수입니다."))
            .storageType(requireNonNull(storageType, "storageType는 필수입니다."))
            .defaultExpirationDays(Math.max(defaultExpirationDays, 0))
            .build();
    }

}