package interview.guide.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/**
 * S3客户端配置（用于RustFS）
 */
@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final StorageConfigProperties storageConfig;

    @Bean
    public S3Client s3Client() {
        // 22. 和你的代码一样：创建账号密码凭证
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                storageConfig.getAccessKey(),
                storageConfig.getSecretKey()
        );

        // ⭐23. 新增：创建S3配置对象（专门用来设置访问样式）
        S3Configuration s3Configuration = S3Configuration.builder()
                // ⭐25. 显式启用虚拟主机样式（OSS要求的访问方式）
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(storageConfig.getEndpoint()))  // 26. 和你的代码一样：设置OSS的endpoint
                // ⭐27. 关键修改：把OSS地域（cn-beijing）转换成AWS兼容的地域（cn-north-1），避免签名错误
                .region(Region.of(convertOssRegionToAwsRegion(storageConfig.getRegion())))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))  // 28. 和你的代码一样：传账号密码
                // ⭐29. 新增：把上面的S3配置（访问样式）传给客户端
                .forcePathStyle(false)
                .serviceConfiguration(s3Configuration)
                .build();  // 30. 构建客户端
    }

    // ⭐31. 新增辅助方法：OSS地域 → AWS地域转换（比如cn-beijing→cn-north-1）
    private String convertOssRegionToAwsRegion(String ossRegion) {
        // 32. 按OSS地域匹配对应的AWS Region（解决地域不匹配导致的签名错误）
        return switch (ossRegion) {
            case "cn-beijing" -> "cn-north-1";
            case "cn-shanghai" -> "cn-east-1";
            case "cn-guangzhou" -> "cn-south-1";
            case "cn-hangzhou" -> "cn-east-2";
            case "cn-shenzhen" -> "cn-south-2";
            case "cn-chengdu" -> "cn-west-1";
            case "cn-hongkong" -> "ap-east-1";
            default -> ossRegion; // 没有匹配的地域就用原值，避免报错
        };
    }
}
