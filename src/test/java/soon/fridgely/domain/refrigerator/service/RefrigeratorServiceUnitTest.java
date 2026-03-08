package soon.fridgely.domain.refrigerator.service;

import com.navercorp.fixturemonkey.FixtureMonkey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.refrigerator.dto.command.CachedMemberRefrigerators;
import soon.fridgely.domain.refrigerator.dto.command.MemberRefrigeratorKey;
import soon.fridgely.domain.refrigerator.dto.request.RefrigeratorUpdateRequest;
import soon.fridgely.domain.refrigerator.dto.response.InvitationCodeResponse;
import soon.fridgely.domain.refrigerator.dto.response.RefrigeratorMemberResponse;
import soon.fridgely.domain.refrigerator.entity.InvitationCode;
import soon.fridgely.domain.refrigerator.entity.MemberRefrigerator;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.entity.RefrigeratorRole;
import soon.fridgely.global.support.FixtureMonkeyFactory;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static soon.fridgely.global.support.fixture.MemberFixture.member;
import static soon.fridgely.global.support.fixture.MemberRefrigeratorFixture.memberRefrigerator;
import static soon.fridgely.global.support.fixture.RefrigeratorFixture.refrigerator;

@ExtendWith(MockitoExtension.class)
class RefrigeratorServiceUnitTest {

    private static final FixtureMonkey fixtureMonkey = FixtureMonkeyFactory.get();

    @InjectMocks
    private RefrigeratorService refrigeratorService;

    @Mock
    private RefrigeratorManager refrigeratorManager;

    @Mock
    private MemberRefrigeratorLinker memberRefrigeratorLinker;

    @Mock
    private MemberRefrigeratorFinder memberRefrigeratorFinder;

    @Mock
    private InvitationCodeGenerator codeGenerator;

    @Test
    void 냉장고_정보를_수정한다() {
        // given
        var key = fixtureMonkey.giveMeOne(MemberRefrigeratorKey.class);
        var request = fixtureMonkey.giveMeBuilder(RefrigeratorUpdateRequest.class)
            .set("newName", "New Name")
            .sample();

        // when
        refrigeratorService.updateRefrigeratorName(key, request);

        // then
        then(refrigeratorManager).should()
            .update(eq(key.refrigeratorId()), eq("New Name"));
    }

    @Test
    void 초대_코드를_생성하고_반환한다() {
        // given
        var key = fixtureMonkey.giveMeOne(MemberRefrigeratorKey.class);

        String generatedCode = "ABC12345";
        given(codeGenerator.generateUnique()).willReturn(generatedCode);

        InvitationCode expectedInvitationCode = fixtureMonkey.giveMeBuilder(InvitationCode.class)
            .set("code", generatedCode)
            .sample();
        given(refrigeratorManager.refreshInvitationCode(eq(key.refrigeratorId()), eq(generatedCode), any(LocalDateTime.class)))
            .willReturn(expectedInvitationCode);

        // when
        InvitationCodeResponse response = refrigeratorService.generateInvitationCode(key);

        // then
        assertThat(response)
            .extracting("code", "expirationAt")
            .containsExactly(generatedCode, expectedInvitationCode.getExpirationAt());

        then(refrigeratorManager).should()
            .refreshInvitationCode(
                eq(key.refrigeratorId()),
                eq(generatedCode),
                any(LocalDateTime.class)
            );
    }

    @Test
    void 초대_코드를_사용해_냉장고에_참여한다() {
        // given
        long memberId = 100L;
        String code = "VALID_CODE";
        long refrigeratorId = 50L;

        Refrigerator mockRefrigerator = mock(Refrigerator.class);
        given(mockRefrigerator.getId()).willReturn(refrigeratorId);

        given(refrigeratorManager.findByInvitationCode(code))
            .willReturn(mockRefrigerator);

        // when
        refrigeratorService.joinByInvitationCode(memberId, code);

        // then
        then(mockRefrigerator).should()
            .validateInvitationCode(eq(code), any(LocalDateTime.class));

        then(memberRefrigeratorLinker).should()
            .linkMemberToRefrigerator(
                eq(new MemberRefrigeratorKey(memberId, refrigeratorId)),
                eq(RefrigeratorRole.MEMBER)
            );
    }

    @Test
    void 내가_속한_냉장고_목록을_조회한다() {
        // given
        long memberId = 1L;

        List<MemberRefrigerator> memberRefrigerators = List.of(
            createMemberRefrigerator("Fridge1", RefrigeratorRole.OWNER),
            createMemberRefrigerator("Fridge2", RefrigeratorRole.MEMBER)
        );

        CachedMemberRefrigerators cachedRefrigerators = CachedMemberRefrigerators.from(memberId, memberRefrigerators);

        given(memberRefrigeratorFinder.findAllByMemberId(memberId))
            .willReturn(cachedRefrigerators);

        // when
        var responses = refrigeratorService.findAllMyRefrigerators(memberId);

        // then
        assertThat(responses).hasSize(2)
            .extracting("name", "role", "isOwner")
            .containsExactlyInAnyOrder(
                tuple("Fridge1", RefrigeratorRole.OWNER, true),
                tuple("Fridge2", RefrigeratorRole.MEMBER, false)
            );
    }

    @Test
    void 특정_냉장고의_상세_정보를_조회한다() {
        // given
        var key = fixtureMonkey.giveMeOne(MemberRefrigeratorKey.class);

        MemberRefrigerator memberRefrigerator = createMemberRefrigerator("MyFridge", RefrigeratorRole.OWNER);
        given(memberRefrigeratorFinder.findByMemberIdAndRefrigeratorId(key.memberId(), key.refrigeratorId()))
            .willReturn(memberRefrigerator);

        // when
        var response = refrigeratorService.findRefrigerator(key);

        // then
        assertThat(response).isNotNull()
            .extracting("name", "role", "isOwner")
            .containsExactly("MyFridge", RefrigeratorRole.OWNER, true);
    }

    @Test
    void 냉장고_팀원_목록을_조회한다() {
        // given
        var key = fixtureMonkey.giveMeOne(MemberRefrigeratorKey.class);

        List<MemberRefrigerator> memberRefrigerators = List.of(
            createMemberRefrigerator("Fridge1", RefrigeratorRole.OWNER),
            createMemberRefrigerator("Fridge2", RefrigeratorRole.MEMBER)
        );

        given(memberRefrigeratorFinder.findAllMembersByRefrigeratorId(key.refrigeratorId()))
            .willReturn(memberRefrigerators);

        // when
        List<RefrigeratorMemberResponse> responses = refrigeratorService.findAllMembers(key);

        // then
        assertThat(responses).hasSize(2)
            .extracting("role", "isOwner")
            .containsExactlyInAnyOrder(
                tuple(RefrigeratorRole.OWNER, true),
                tuple(RefrigeratorRole.MEMBER, false)
            );
    }

    private MemberRefrigerator createMemberRefrigerator(String fridgeName, RefrigeratorRole role) {
        Member member = member(fixtureMonkey)
            .set("id", fixtureMonkey.giveMeOne(Long.class))
            .sample();
        Refrigerator refrigerator = refrigerator(fixtureMonkey)
            .set("id", fixtureMonkey.giveMeOne(Long.class))
            .set("name", fridgeName)
            .sample();

        return memberRefrigerator(fixtureMonkey, refrigerator, member)
            .set("role", role)
            .sample();
    }

}