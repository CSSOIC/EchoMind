
package interview.guide.infrastructure.file;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunOssStorageService {

    private final OSS ossClient;
    private final AliyunOssConfig config;
    public String getFileUrl(String fileKey) {
        return String.format("%s/%s/%s", config.getEndpoint(), config.getBucketName(), fileKey);
    }
    /**
     * 上传简历文件
     */
    public String uploadResume(MultipartFile file) throws IOException {
        return uploadFile(file);
    }

    /**
     * 删除简历文件
     */
    public void deleteResume(String fileKey) {
        deleteFile(fileKey);
    }

    /**
     * 上传知识库文件
     */
    public String uploadKnowledgeBase(MultipartFile file) throws IOException {
        return uploadFile(file);
    }

    /**
     * 删除知识库文件
     */
    public void deleteKnowledgeBase(String fileKey) {
        deleteFile(fileKey);
    }

    /**
     * 上传文件到阿里云 OSS
     */
    public String uploadFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        // 生成唯一的文件路径
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String objectKey = datePath + "/" + UUID.randomUUID() + extension;

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest request = new PutObjectRequest(
                    config.getBucketName(),
                    objectKey,
                    new ByteArrayInputStream(file.getBytes()),
                    metadata
            );

            ossClient.putObject(request);

            String fileUrl = "https://" + config.getBucketName() + "." +
                    getEndpointHost() + "/" + objectKey;

            log.info("文件已上传到阿里云 OSS: {}, URL: {}", objectKey, fileUrl);
            return fileUrl;

        } catch (Exception e) {
            log.error("上传文件到阿里云 OSS 失败：{}", e.getMessage(), e);
            throw new IOException("上传文件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 从 OSS 下载文件
     */
    public byte[] downloadFile(String objectKey) throws IOException {
        try {
            var ossObject = ossClient.getObject(config.getBucketName(), objectKey);
            return ossObject.getObjectContent().readAllBytes();
        } catch (Exception e) {
            log.error("从 OSS 下载文件失败：{}", e.getMessage(), e);
            throw new IOException("下载文件失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除 OSS 上的文件
     */
    public void deleteFile(String objectKey) {
        try {
            ossClient.deleteObject(config.getBucketName(), objectKey);
            log.info("文件已删除：{}", objectKey);
        } catch (Exception e) {
            log.error("删除文件失败：{}", e.getMessage(), e);
        }
    }

    private String getEndpointHost() {
        String endpoint = config.getEndpoint();
        if (endpoint.startsWith("http://")) {
            return endpoint.substring(7);
        } else if (endpoint.startsWith("https://")) {
            return endpoint.substring(8);
        }
        return endpoint;
    }
}
