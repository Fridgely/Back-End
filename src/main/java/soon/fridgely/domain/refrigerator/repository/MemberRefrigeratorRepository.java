package soon.fridgely.domain.refrigerator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;

import java.util.List;
import java.util.Optional;

public interface MemberRefrigeratorRepository extends JpaRepository<MemberRefrigerator, Long> {

    Optional<MemberRefrigerator> findByMemberAndRefrigerator(Member member, Refrigerator refrigerator);

    List<MemberRefrigerator> findByMemberAndRoleAndStatus(Member member, RefrigeratorRole role, EntityStatus status);

    boolean existsByRefrigeratorIdAndMemberIdAndStatus(long refrigeratorId, long memberId, EntityStatus status);

    @Query("""
            SELECT mr FROM MemberRefrigerator mr
            JOIN FETCH mr.refrigerator r
            WHERE mr.member.id = :memberId
            AND mr.status = :status
            AND r.status = :status
            ORDER BY mr.createdAt ASC
        """)
    List<MemberRefrigerator> findAllMyRefrigerators(
        @Param("memberId") long memberId,
        @Param("status") EntityStatus status
    );

    @Query("""
            SELECT mr FROM MemberRefrigerator mr
            JOIN FETCH mr.refrigerator r
            WHERE mr.member.id = :memberId 
            AND mr.refrigerator.id = :refrigeratorId
            AND mr.status = :status
            AND r.status = :status
        """)
    Optional<MemberRefrigerator> findByMemberIdAndRefrigeratorId(
        @Param("memberId") long memberId,
        @Param("refrigeratorId") long refrigeratorId,
        @Param("status") EntityStatus status
    );

}