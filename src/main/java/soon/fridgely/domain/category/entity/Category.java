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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CategoryType type;

    public static Category register(String name, Refrigerator refrigerator, Member member, CategoryType categoryType) {
        return Category.builder()
            .name(requireNonNull(name, "name은 필수입니다."))
            .refrigerator(requireNonNull(refrigerator, "refrigerator는 필수입니다."))
            .member(requireNonNull(member, "member는 필수입니다."))
            .type(requireNonNull(categoryType, "categoryType은 필수입니다."))
            .build();
    }

    public boolean isDefaultType() {
        return this.type == CategoryType.DEFAULT;
    }

    public boolean isSameName(String name) {
        return this.name.equals(name);
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name는 비어 있을 수 없습니다.");
        }

        this.name = name;
    }

}
