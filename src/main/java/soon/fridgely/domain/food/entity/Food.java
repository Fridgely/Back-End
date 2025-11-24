package soon.fridgely.domain.food.entity;

import jakarta.persistence.*;
import lombok.*;
import soon.fridgely.domain.BaseEntity;
import soon.fridgely.domain.category.entity.Category;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static java.util.Objects.requireNonNull;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "foods")
@Entity
public class Food extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refrigerator_id", nullable = false)
    private Refrigerator refrigerator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Embedded
    private Quantity quantity;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StorageType storageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FoodStatus foodStatus;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 512)
    private String imageURL;

    public static Food register(
        Refrigerator refrigerator,
        Member member,
        String name,
        Category category,
        Quantity quantity,
        LocalDateTime expirationDate,
        StorageType storageType,
        String description,
        String imageURL,
        LocalDate now
    ) {
        return Food.builder()
            .refrigerator(requireNonNull(refrigerator, "refrigerator는 필수입니다."))
            .member(requireNonNull(member, "member는 필수입니다."))
            .name(requireNonNull(name, "name은 필수입니다."))
            .category(requireNonNull(category, "category는 필수입니다."))
            .quantity(requireNonNull(quantity, "quantity는 필수입니다."))
            .expirationDate(requireNonNull(expirationDate, "expirationDate는 필수입니다."))
            .storageType(requireNonNull(storageType, "storageType는 필수입니다."))
            .foodStatus(FoodStatus.fromDaysLeft(expirationDate.toLocalDate(), now))
            .description(description == null ? "" : description)
            .imageURL(imageURL == null ? "" : imageURL)
            .build();
    }

    public void update(
        String name,
        Category category,
        Quantity quantity,
        LocalDateTime expirationDate,
        StorageType storageType,
        String description,
        String imageURL,
        LocalDate now
    ) {
        this.name = requireNonNull(name, "name은 필수입니다.");
        this.quantity = requireNonNull(quantity, "quantity는 필수입니다.");
        this.expirationDate = requireNonNull(expirationDate, "expirationDate는 필수입니다.");
        this.storageType = requireNonNull(storageType, "storageType는 필수입니다.");
        this.description = description == null ? "" : description;
        this.foodStatus = FoodStatus.fromDaysLeft(expirationDate.toLocalDate(), now);

        if (category != null) {
            this.category = category;
        }

        if (imageURL != null && !imageURL.isBlank()) {
            this.imageURL = imageURL;
        }
    }

    /**
     * 유통기한까지 남은 일수를 계산
     * 양수: 남음, 음수: 지남, 0: 당일
     */
    public long calculateDaysLeft(LocalDate now) {
        requireNonNull(now, "now는 필수입니다.");
        return ChronoUnit.DAYS.between(now, this.expirationDate.toLocalDate());
    }

}