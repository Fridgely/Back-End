package soon.fridgely.global.infra.provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import soon.fridgely.global.infra.properties.S3Properties;
import soon.fridgely.global.support.exception.CoreException;
import soon.fridgely.global.support.exception.ErrorType;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class S3ProviderUnitTest {

    private S3Provider s3Provider;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private S3Properties s3Properties;

    @Mock
    private S3Utilities s3Utilities;

    @BeforeEach
    void setUp() {
        S3Properties.S3 s3Config = new S3Properties.S3("test-bucket");
        given(s3Properties.s3()).willReturn(s3Config);
        s3Provider = new S3Provider(s3Client, s3Presigner, s3Properties);
    }

    @Test
    void contentType이_명시된_경우_파일을_업로드_한다() throws Exception {
        // given
        String contentType = "image/jpeg";
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/images/test.jpg";

        given(s3Client.utilities()).willReturn(s3Utilities);
        given(s3Utilities.getUrl(any(GetUrlRequest.class))).willReturn(new URL(expectedUrl));

        // when
        String actualUrl = s3Provider.upload("images/test.jpg", new ByteArrayInputStream(new byte[1024]), 1024L, contentType);

        // then
        assertThat(actualUrl).isEqualTo(expectedUrl);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        then(s3Client).should()
            .putObject(requestCaptor.capture(), any(RequestBody.class));

        assertThat(requestCaptor.getValue().contentType()).isEqualTo(contentType);
    }

    @Test
    void 알_수_없는_확장자는_application_octet_stream으로_추론한다() throws Exception {
        // given
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/files/unknown";

        given(s3Client.utilities()).willReturn(s3Utilities);
        given(s3Utilities.getUrl(any(GetUrlRequest.class))).willReturn(new URL(expectedUrl));

        // when
        s3Provider.upload("files/unknown", new ByteArrayInputStream(new byte[100]), 100L, null);

        // then
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        then(s3Client).should()
            .putObject(requestCaptor.capture(), any(RequestBody.class));

        assertThat(requestCaptor.getValue().contentType()).isEqualTo("application/octet-stream");
    }

    @Test
    void 파일_업로드에_실패_할_경우_예외가_발생한다() {
        // given
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .willThrow(S3Exception.builder().message("S3 upload failed").build());

        // expected
        assertThatThrownBy(() -> s3Provider.upload("images/test.jpg", new ByteArrayInputStream(new byte[1024]), 1024L, "image/jpeg"))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.STORAGE_UPLOAD_FAILED);
    }

    @Test
    void 파일을_삭제한다() {
        // given
        String key = "images/to-delete.jpg";

        // when
        s3Provider.delete(key);

        // then
        then(s3Client).should(times(1))
            .deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void presigned_URL을_생성한다() throws Exception {
        // given
        String key = "images/test.jpg";
        Duration expiration = Duration.ofMinutes(15);
        String expectedUrl = "https://test-bucket.s3.amazonaws.com/images/test.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Expires=900";

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        given(presignedRequest.url()).willReturn(new URL(expectedUrl));
        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
            .willReturn(presignedRequest);

        // when
        String actualUrl = s3Provider.generatePresignedUrl(key, expiration);

        // then
        assertThat(actualUrl).isEqualTo(expectedUrl);
        then(s3Presigner).should(times(1))
            .presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void presigned_URL_생성에_실패할_경우_예외가_발생한다() {
        // given
        String key = "images/test.jpg";
        Duration expiration = Duration.ofMinutes(15);

        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
            .willThrow(S3Exception.builder().message("Presigner failed").build());

        // expected
        assertThatThrownBy(() -> s3Provider.generatePresignedUrl(key, expiration))
            .isInstanceOf(CoreException.class)
            .extracting("errorType")
            .isEqualTo(ErrorType.STORAGE_PRESIGNED_URL_FAILED);
    }

}