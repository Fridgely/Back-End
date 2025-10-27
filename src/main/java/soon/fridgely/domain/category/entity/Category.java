package soon.fridgely.domain.category.entity;

import jakarta.persistence.*;
import lombok.*;
import soon.fridgely.domain.BaseEntity;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;

import static java.util.Objects.requireNonNull;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
    name = "categories",
    uniqueConstraints = @UniqueConstraint(name = "uk_categories_refrigerator_id_name", columnNames = {"refrigerator_id", "name"})
)
@Entity
public class Category extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refrigerator_id", nullable = false)
    private Refrigerator refrigerator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public static Category register(String name, Refrigerator refrigerator, Member member) {
        return Category.builder()
            .name(requireNonNull(name, "name는 필수입니다."))
            .refrigerator(requireNonNull(refrigerator, "refrigerator는 필수입니다."))
            .member(requireNonNull(member, "member는 필수입니다."))
            .build();
    }

}
