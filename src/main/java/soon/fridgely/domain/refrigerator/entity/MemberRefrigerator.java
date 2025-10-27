package soon.fridgely.domain.refrigerator.entity;

import jakarta.persistence.*;
import lombok.*;
import soon.fridgely.domain.BaseEntity;
import soon.fridgely.domain.member.entity.Member;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
    name = "member_refrigerators",
    uniqueConstraints = @UniqueConstraint(name = "uk_member_refrigerators_member_id_refrigerator_id", columnNames = {"member_id", "refrigerator_id"})
)
@Entity
public class MemberRefrigerator extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refrigerator_id", nullable = false)
    private Refrigerator refrigerator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefrigeratorRole role;

    public static MemberRefrigerator link(Member member, Refrigerator refrigerator, RefrigeratorRole role) {
        return MemberRefrigerator.builder()
            .member(member)
            .refrigerator(refrigerator)
            .role(role)
            .build();
    }

}