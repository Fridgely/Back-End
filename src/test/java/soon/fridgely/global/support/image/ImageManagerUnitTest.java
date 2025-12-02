package soon.fridgely.global.support.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import soon.fridgely.global.infra.provider.StorageProvider;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ImageManagerUnitTest {

    @InjectMocks
    private ImageManager imageManager;

    @Mock
    private StorageProvider storageProvider;

    // JPEG 매직 넘버
    private static final byte[] REAL_JPEG_HEADER = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00};
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Test
    void 파일_헤더가_유효한_실제_이미지는_업로드_한다() {
        // given
        MockMultipartFile validFile = new MockMultipartFile(
            "image", "test.jpg", "image/jpeg", REAL_JPEG_HEADER
        );

        String expectedUrl = "https://s3.example.com/images/uuid-test.jpg";
        given(storageProvider.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
            .willReturn(expectedUrl);

        // when
        String actualUrl = imageManager.upload(validFile);

        // then
        assertThat(actualUrl).isEqualTo(expectedUrl);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        then(storageProvider).should()
            .upload(
                captor.capture(),
                any(InputStream.class),
                eq(validFile.getSize()),
                eq("image/jpeg")
            );

        assertThat(captor.getValue())
            .startsWith("images/")
            .endsWith("-test.jpg");
    }

    @Test
    void 내용이_위변조된_파일은_예외가_발생한다() {
        // given
        byte[] fakeContent = "This is a virus script".getBytes();
        MockMultipartFile fakeFile = new MockMultipartFile(
            "image", "hack.jpg", "image/jpeg", fakeContent
        );

        // expected
        assertThatThrownBy(() -> imageManager.upload(fakeFile))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_FILE_TYPE);

        then(storageProvider).shouldHaveNoInteractions();
    }

    @Test
    void 지원하지_않는_확장자나_content_type은_예외가_발생한다() {
        // given
        MockMultipartFile pdfFile = new MockMultipartFile(
            "file", "document.pdf", "application/pdf", new byte[]{1, 2, 3}
        );

        // expected
        assertThatThrownBy(() -> imageManager.upload(pdfFile))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_FILE_TYPE);
    }

    @Test
    void 파일_크기가_10MB를_초과하면_예외가_발생한다() {
        // given
        byte[] largeContent = new byte[(int) MAX_FILE_SIZE + 1];
        MockMultipartFile largeFile = new MockMultipartFile(
            "image", "large.jpg", "image/jpeg", largeContent
        );

        // expected
        assertThatThrownBy(() -> imageManager.upload(largeFile))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.FILE_SIZE_EXCEEDED);
    }

    @Test
    void 파일이_null이거나_비어있으면_null을_반환한다() {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile("image", "", "image/jpeg", new byte[0]);

        // when
        assertThat(imageManager.upload(null)).isNull();
        assertThat(imageManager.upload(emptyFile)).isNull();

        // then
        then(storageProvider).should(never())
            .upload(any(), any(), anyLong(), any());
    }

}