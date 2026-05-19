package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import soon.fridgely.domain.EntityStatus;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.dto.response.RefrigeratorResponse;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.domain.refrigerator.repository.MemberRefrigeratorRepository;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.IntegrationTestSupport;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

class RefrigeratorServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RefrigeratorService refrigeratorService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Autowired
    private MemberRefrigeratorRepository memberRefrigeratorRepository;

    private Member member;
    private Refrigerator refrigerator;

    @BeforeEach
    void setUp() {
        this.member = memberRepository.save(member(fixtureMonkey).sample());
        this.refrigerator = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, member)
                .set("role", RefrigeratorRole.OWNER)
                .sample()
        );
    }

    // ======================== 냉장고 등록 ========================

    @Test
    void 냉장고를_생성한다() {
        // given
        Member newMember = memberRepository.save(
            member(fixtureMonkey).set("nickname", "nickname").sample()
        );

        // when
        Refrigerator saved = refrigeratorService.register(newMember);

        // then
        assertThat(saved).extracting("name").isEqualTo("nickname의 냉장고");
    }

    // ======================== 냉장고 이름 수정 ========================

    @Test
    void 냉장고_이름을_수정한다() {
        // given
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());
        String newName = "새로운 이름";

        // when
        refrigeratorService.updateRefrigeratorName(key, new soon.fridgely.domain.refrigerator.dto.request.RefrigeratorUpdateRequest(newName));

        // then
        Refrigerator updated = refrigeratorRepository.findById(refrigerator.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo(newName);
    }

    // ======================== 초대 코드 생성 ========================

    @Test
    void 초대_코드를_생성하고_저장한다() {
        // given
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());
        Pattern validPattern = Pattern.compile("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$");

        // when
        var response = refrigeratorService.generateInvitationCode(key);

        // then
        assertThat(response.code()).matches(validPattern);
        assertThat(response.expirationAt()).isAfter(LocalDateTime.now());
    }

    // ======================== 초대 코드로 참여 ========================

    @Test
    void 유효한_초대_코드로_냉장고에_참여한다() {
        // given
        Member guestMember = memberRepository.save(member(fixtureMonkey).sample());
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());
        var response = refrigeratorService.generateInvitationCode(key);

        // when
        refrigeratorService.joinByInvitationCode(guestMember.getId(), response.code());

        // then
        assertThat(memberRefrigeratorRepository.existsByRefrigeratorIdAndMemberIdAndStatus(
            refrigerator.getId(), guestMember.getId(), EntityStatus.ACTIVE)).isTrue();
    }

    @Test
    void 이미_가입한_냉장고에_다시_참여하려하면_예외가_발생한다() {
        // given
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());
        var response = refrigeratorService.generateInvitationCode(key);

        // expected
        assertThatThrownBy(() -> refrigeratorService.joinByInvitationCode(member.getId(), response.code()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.ALREADY_JOINED_REFRIGERATOR);
    }

    // ======================== 냉장고 목록 조회 ========================

    @Test
    void 내가_속한_모든_냉장고_목록을_조회한다() {
        // given
        Member otherMember = memberRepository.save(member(fixtureMonkey).sample());
        Refrigerator myRefrigerator2 = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());
        Refrigerator otherFridge = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());

        memberRefrigeratorRepository.saveAll(List.of(
            memberRefrigerator(fixtureMonkey, myRefrigerator2, member)
                .set("role", RefrigeratorRole.MEMBER).sample(),
            memberRefrigerator(fixtureMonkey, otherFridge, otherMember).sample()
        ));

        // when
        List<RefrigeratorResponse> result = refrigeratorService.findAllMyRefrigerators(member.getId());

        // then
        assertThat(result).hasSize(2)
            .extracting(RefrigeratorResponse::id)
            .containsExactlyInAnyOrder(refrigerator.getId(), myRefrigerator2.getId());
    }

    // ======================== 멤버십 조회 ========================

    @Test
    void 특정_냉장고에_대한_멤버십_정보를_조회한다() {
        // when
        MemberRefrigerator result = refrigeratorService.findMemberRefrigerator(member.getId(), refrigerator.getId());

        // then
        assertThat(result).isNotNull()
            .extracting("refrigerator.id", "role")
            .containsExactly(refrigerator.getId(), RefrigeratorRole.OWNER);
    }

    @Test
    void 멤버십_정보가_존재하지_않으면_예외가_발생한다() {
        // given
        Member outsider = memberRepository.save(member(fixtureMonkey).sample());

        // expected
        assertThatThrownBy(() -> refrigeratorService.findMemberRefrigerator(outsider.getId(), refrigerator.getId()))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA);
    }

    @Test
    void 냉장고와_멤버의_연결을_확인한다() {
        // when
        boolean exists = memberRefrigeratorRepository.existsByRefrigeratorIdAndMemberIdAndStatus(
            refrigerator.getId(), member.getId(), EntityStatus.ACTIVE);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void 연결되지_않은_냉장고는_false를_반환한다() {
        // given
        Member outsider = memberRepository.save(member(fixtureMonkey).sample());

        // when
        boolean exists = memberRefrigeratorRepository.existsByRefrigeratorIdAndMemberIdAndStatus(
            refrigerator.getId(), outsider.getId(), EntityStatus.ACTIVE);

        // then
        assertThat(exists).isFalse();
    }

    // ======================== 팀원 목록 조회 ========================

    @Test
    void 냉장고에_속한_팀원_목록을_조회한다() {
        // given
        Member guestMember = memberRepository.save(member(fixtureMonkey).sample());
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, guestMember)
                .set("role", RefrigeratorRole.MEMBER).sample()
        );
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        // when
        var result = refrigeratorService.findAllMembers(key);

        // then
        assertThat(result).hasSize(2)
            .extracting("memberId")
            .containsExactlyInAnyOrder(member.getId(), guestMember.getId());
    }

    @Test
    void 팀원이_없는_냉장고는_소유자만_반환한다() {
        // given
        Refrigerator emptyFridge = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, emptyFridge, member)
                .set("role", RefrigeratorRole.OWNER).sample()
        );
        // emptyFridge를 setUp의 member가 소유 (멤버십만 있고 게스트 없음)
        var key = new MemberRefrigeratorKey(member.getId(), emptyFridge.getId());

        // when
        var result = refrigeratorService.findAllMembers(key);

        // then
        assertThat(result).hasSize(1); // OWNER 본인만 존재
    }

    @Test
    void MemberRefrigerator가_삭제_상태이면_팀원_목록에서_제외된다() {
        // given
        Member guestMember = memberRepository.save(member(fixtureMonkey).sample());
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, guestMember)
                .set("role", RefrigeratorRole.MEMBER)
                .set("status", EntityStatus.DELETED)
                .sample()
        );
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        // when
        var result = refrigeratorService.findAllMembers(key);

        // then
        assertThat(result).hasSize(1); // OWNER만 조회됨
    }

    // ======================== 소유자로 연결 ========================

    @Test
    void 회원과_냉장고를_OWNER로_연결한다() {
        // given
        Member newMember = memberRepository.save(member(fixtureMonkey).sample());
        Refrigerator newRefrigerator = refrigeratorRepository.save(refrigerator(fixtureMonkey).sample());

        // when
        refrigeratorService.linkToOwner(newMember, newRefrigerator);

        // then
        MemberRefrigerator link = memberRefrigeratorRepository
            .findByMemberAndRefrigerator(newMember, newRefrigerator).orElseThrow();
        assertThat(link)
            .extracting("member.id", "refrigerator.id", "role")
            .containsExactly(newMember.getId(), newRefrigerator.getId(), RefrigeratorRole.OWNER);
    }

    // ======================== 냉장고 나가기 ========================

    @Test
    void MEMBER가_냉장고를_나가면_연결이_해제된다() {
        // given
        Member guestMember = memberRepository.save(member(fixtureMonkey).sample());
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, guestMember)
                .set("role", RefrigeratorRole.MEMBER).sample()
        );
        var key = new MemberRefrigeratorKey(guestMember.getId(), refrigerator.getId());

        // when
        refrigeratorService.leaveRefrigerator(key);

        // then
        assertThat(memberRefrigeratorRepository.existsByRefrigeratorIdAndMemberIdAndStatus(
            refrigerator.getId(), guestMember.getId(), EntityStatus.ACTIVE)).isFalse();
    }

    @Test
    void 이미_나간_냉장고에_다시_나가기를_시도해도_예외가_발생하지_않는다() {
        // given
        Member guestMember = memberRepository.save(member(fixtureMonkey).sample());
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, guestMember)
                .set("role", RefrigeratorRole.MEMBER).sample()
        );
        var key = new MemberRefrigeratorKey(guestMember.getId(), refrigerator.getId());
        refrigeratorService.leaveRefrigerator(key);

        // expected
        assertThatNoException().isThrownBy(() -> refrigeratorService.leaveRefrigerator(key));
    }

    @Test
    void OWNER가_냉장고를_나가려하면_예외가_발생한다() {
        // given
        var key = new MemberRefrigeratorKey(member.getId(), refrigerator.getId());

        // expected
        assertThatThrownBy(() -> refrigeratorService.leaveRefrigerator(key))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.OWNER_CANNOT_LEAVE_REFRIGERATOR);

        assertThat(memberRefrigeratorRepository.existsByRefrigeratorIdAndMemberIdAndStatus(
            refrigerator.getId(), member.getId(), EntityStatus.ACTIVE)).isTrue();
    }

    // ======================== 전체 멤버 해제 ========================

    @Test
    void 냉장고의_모든_멤버_연결을_해제한다() {
        // given
        Member guestMember = memberRepository.save(member(fixtureMonkey).sample());
        memberRefrigeratorRepository.save(
            memberRefrigerator(fixtureMonkey, refrigerator, guestMember)
                .set("role", RefrigeratorRole.MEMBER).sample()
        );

        // when
        refrigeratorService.unlinkAll(refrigerator.getId());

        // then
        assertThat(memberRefrigeratorRepository.existsByRefrigeratorIdAndMemberIdAndStatus(
            refrigerator.getId(), member.getId(), EntityStatus.ACTIVE)).isFalse();
        assertThat(memberRefrigeratorRepository.existsByRefrigeratorIdAndMemberIdAndStatus(
            refrigerator.getId(), guestMember.getId(), EntityStatus.ACTIVE)).isFalse();
    }

    // ======================== 냉장고 삭제 ========================

    @Test
    void 냉장고를_삭제하면_DELETED_상태가_된다() {
        // when
        refrigeratorService.delete(refrigerator.getId());

        // then
        assertThat(refrigeratorRepository.findByIdAndStatus(refrigerator.getId(), EntityStatus.ACTIVE)).isEmpty();
    }

    @Test
    void 이미_삭제된_냉장고를_다시_삭제해도_예외가_발생하지_않는다() {
        // given
        refrigeratorService.delete(refrigerator.getId());

        // expected
        assertThatNoException().isThrownBy(() -> refrigeratorService.delete(refrigerator.getId()));
    }
}
