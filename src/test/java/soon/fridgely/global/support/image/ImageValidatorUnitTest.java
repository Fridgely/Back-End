package soon.fridgely.global.support.image;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageValidatorUnitTest {

    private final ImageValidator validator = new ImageValidator();

    @Test
    void 유효한_JPEG_파일은_검증을_통과한다() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "image", "photo.jpg", "image/jpeg", createValidJpeg()
        );

        // expected
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void 유효한_PNG_파일은_검증을_통과한다() throws IOException {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "image", "photo.png", "image/png", createValidPng()
        );

        // expected
        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void 매직넘버는_통과하지만_파싱_불가한_폴리글랏_파일은_예외가_발생한다() {
        // given
        byte[] content = new byte[200];
        content[0] = (byte) 0xFF;
        content[1] = (byte) 0xD8;
        content[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile(
            "image", "evil.jpg", "image/jpeg", content
        );

        // expected
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_FILE_TYPE);
    }

    @Test
    void gif_확장자는_예외가_발생한다() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "image", "anim.gif", "image/gif",
            new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}
        );

        // expected
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_FILE_TYPE);
    }

    @Test
    void webp_확장자는_예외가_발생한다() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "image", "photo.webp", "image/webp",
            new byte[]{0x52, 0x49, 0x46, 0x46}
        );

        // expected
        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_FILE_TYPE);
    }

    private byte[] createValidJpeg() throws IOException {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", baos);
        return baos.toByteArray();
    }

    private byte[] createValidPng() throws IOException {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
