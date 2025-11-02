package soon.fridgely.domain.barcode.entity;

import jakarta.persistence.*;
import lombok.*;
import soon.fridgely.domain.BaseEntity;
import soon.fridgely.domain.food.entity.StorageType;
import soon.fridgely.domain.member.entity.Member;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "barcode_inputs")
@Entity
public class BarcodeInput extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barcode_id")
    private Barcode barcode;

    @Column(nullable = false, length = 128)
    private String rawCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StorageType storageType;

    @Column(nullable = false)
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    @Column
    private LocalDateTime verifiedAt;

    public static BarcodeInput register(
        Barcode barcode,
        String rawCode,
        Member member,
        String name,
        String category,
        StorageType storageType,
        LocalDate expirationDate
    ) {
        return BarcodeInput.builder()
            .barcode(barcode)
            .rawCode(requireNonNull(rawCode))
            .member(requireNonNull(member))
            .name(requireNonNull(name))
            .category(requireNonNull(category))
            .storageType(requireNonNull(storageType))
            .expirationDate(requireNonNull(expirationDate))
            .verificationStatus(VerificationStatus.PENDING)
            .build();
    }

}