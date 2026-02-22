package soon.fridgely.domain.refrigerator.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import soon.fridgely.domain.refrigerator.repository.RefrigeratorRepository;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvitationCodeGeneratorUnitTest {

    @InjectMocks
    private InvitationCodeGenerator generator;

    @Mock
    private RefrigeratorRepository refrigeratorRepository;

    @Test
    void 생성된_코드는_8자리이며_대문자와_숫자로_구성되어있다() {
        // given
        Pattern validPattern = Pattern.compile("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$");
        given(refrigeratorRepository.existsByInvitationCode_code(anyString())).willReturn(false);

        // when
        String code = generator.generateUnique();

        // then
        assertThat(code).matches(validPattern);
    }

    @Test
    void 중복되지_않은_코드가_생성되면_첫_시도에_성공한다() {
        // given
        given(refrigeratorRepository.existsByInvitationCode_code(anyString())).willReturn(false);

        // when
        String code = generator.generateUnique();

        // then
        assertThat(code).isNotNull();
        verify(refrigeratorRepository, times(1)).existsByInvitationCode_code(anyString());
    }

    @Test
    void 중복이_발생하면_재시도하여_고유한_코드를_생성한다() {
        // given
        given(refrigeratorRepository.existsByInvitationCode_code(anyString()))
            .willReturn(true, true, false);

        // when
        String code = generator.generateUnique();

        // then
        assertThat(code).isNotNull();
        verify(refrigeratorRepository, times(3)).existsByInvitationCode_code(anyString());
    }

    @Test
    void 열번_재시도_후에도_중복이면_예외를_발생시킨다() {
        // given
        given(refrigeratorRepository.existsByInvitationCode_code(anyString())).willReturn(true);

        // expected
        assertThatThrownBy(() -> generator.generateUnique())
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.INVITATION_CODE_GENERATION_FAILED);

        verify(refrigeratorRepository, times(10)).existsByInvitationCode_code(anyString());
    }

}