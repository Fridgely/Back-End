package soon.fridgely.domain.member.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import soon.fridgely.IntegrationTestSupport;
import soon.fridgely.domain.member.dto.MemberInfo;
import soon.fridgely.domain.member.entity.Member;
import soon.fridgely.domain.member.repository.MemberRepository;
import soon.fridgely.domain.refrigerator.entity.Refrigerator;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.domain.refrigerator.service.MemberRefrigeratorLinker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

class MemberServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberService memberService;

    @MockitoBean
    private MemberRefrigeratorLinker memberRefrigeratorLinker;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefrigeratorRepository refrigeratorRepository;

    @Test
    void 회원가입이_성공하면_모든_데이터가_저장되고_연결이_호출된다() {
        // given
        MemberInfo memberInfo = new MemberInfo("testId", "testPassword", "testNickname");

        // when
        Long memberId = memberService.register(memberInfo);

        // then
        Member member = memberRepository.findById(memberId).orElseThrow();
        assertThat(member.getLoginId()).isEqualTo("testId");

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        ArgumentCaptor<Refrigerator> fridgeCaptor = ArgumentCaptor.forClass(Refrigerator.class);
        then(memberRefrigeratorLinker).should()
            .linkToOwner(memberCaptor.capture(), fridgeCaptor.capture());
        assertThat(memberCaptor.getValue().getId()).isEqualTo(memberId);

        Refrigerator capturedFridge = fridgeCaptor.getValue();
        assertThat(capturedFridge).isNotNull();
        assertThat(capturedFridge.getId()).isNotNull();
        assertThat(capturedFridge.getName()).isEqualTo("testNickname" + "의 냉장고");
    }

    @Test
    void 회원가입_중_연결이_실패하면_모든_작업이_롤백된다() {
        // given
        MemberInfo memberInfo = new MemberInfo("testId", "testPassword", "testNickname");

        willThrow(new RuntimeException("DB 연결 실패"))
            .given(memberRefrigeratorLinker).linkToOwner(any(), any());

        // when
        assertThatThrownBy(() -> memberService.register(memberInfo))
            .isInstanceOf(RuntimeException.class);

        // then
        assertThat(memberRepository.count()).isEqualTo(0);
        assertThat(refrigeratorRepository.count()).isEqualTo(0);
    }

}