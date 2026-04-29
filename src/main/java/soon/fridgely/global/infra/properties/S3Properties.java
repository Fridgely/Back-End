package soon.fridgely.global.infra.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cloud.aws")
public record S3Properties(
    @Valid Credentials credentials,
    @Valid Region region,
    @Valid S3 s3
) {

    public record Credentials(
        @NotBlank(message = "AWS accessKey는 필수입니다.") String accessKey,
        @NotBlank(message = "AWS secretKey는 필수입니다.") String secretKey
    ) {
    }

    public record Region(
        @NotBlank(message = "AWS region은 필수입니다.") String staticRegion
    ) {
    }

    public record S3(
        @NotBlank(message = "S3 bucket은 필수입니다.") String bucket
    ) {
    }

}