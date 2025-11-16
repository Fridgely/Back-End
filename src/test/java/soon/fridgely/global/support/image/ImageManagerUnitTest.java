package soon.fridgely.global.support.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;
import soon.fridgely.global.support.provider.StorageProvider;

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

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Test
    void 이미지_업로드에_성공하면_S3_URL이_반환된다() {
        // given
        MockMultipartFile mockFile = createMockFile("image", "test-image.png", "image/png", MAX_FILE_SIZE - 1);
        String expectedUrl = "https://s3.example.com/images/uuid-test-image.png";

        given(storageProvider.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
            .willReturn(expectedUrl);

        // when
        String actualUrl = imageManager.upload(mockFile);

        // then
        assertThat(actualUrl).isEqualTo(expectedUrl);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        then(storageProvider).should()
            .upload(
                captor.capture(),
                any(InputStream.class),
                eq(mockFile.getSize()),
                eq(mockFile.getContentType())
            );

        assertThat(captor.getValue())
            .startsWith("images/")
            .endsWith("-test-image.png");
    }

    @Test
    void 파일이_null이라면_null이_반환된다() {
        // given
        MockMultipartFile mockFile = null;

        // when
        String actualUrl = imageManager.upload(mockFile);

        // then
        assertThat(actualUrl).isNull();
        then(storageProvider).should(never())
            .upload(any(), any(), anyLong(), any());
    }

    @Test
    void 파일이_비어있다면_null이_반환된다() {
        // given
        MockMultipartFile mockFile = createMockFile("image", "", "image/png", 0L);

        // when
        String actualUrl = imageManager.upload(mockFile);

        // then
        assertThat(mockFile.isEmpty()).isTrue();
        assertThat(actualUrl).isNull();

        then(storageProvider).should(never())
            .upload(any(), any(), anyLong(), any());
    }

    @Test
    void 지원하지_않는_파일_타입은_예외가_발생한다() {
        // given
        MockMultipartFile mockFile = createMockFile("file", "document.pdf", "application/pdf", 1024L);

        // expected
        assertThatThrownBy(() -> imageManager.upload(mockFile))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST);
    }

    @Test
    void 최대_파일_크기를_초과하면_예외가_발생한다() {
        // given
        MockMultipartFile mockFile = createMockFile("image", "large.png", "image/png", MAX_FILE_SIZE + 1);

        // expected
        assertThatThrownBy(() -> imageManager.upload(mockFile))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST);
    }

    private MockMultipartFile createMockFile(String name, String originalFilename, String contentType, long size) {
        byte[] content = new byte[(int) size];
        return new MockMultipartFile(name, originalFilename, contentType, content);
    }

}