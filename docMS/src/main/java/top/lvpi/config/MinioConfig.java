package top.lvpi.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.GetPresignedObjectUrlArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class MinioConfig {
    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.rootUser}")
    private String accessKey;

    @Value("${minio.rootPassword}")
    private String secretKey;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Bean
    public MinioClient minioClient() {
        try {
            // 验证配置值
            if (accessKey == null || accessKey.trim().isEmpty()) {
                throw new IllegalArgumentException("MinIO accessKey (rootUser) is not configured");
            }
            if (secretKey == null || secretKey.trim().isEmpty()) {
                throw new IllegalArgumentException("MinIO secretKey (rootPassword) is not configured");
            }
            if (endpoint == null || endpoint.trim().isEmpty()) {
                throw new IllegalArgumentException("MinIO endpoint is not configured");
            }

            MinioClient minioClient = MinioClient.builder()
                    .endpoint("http://" + endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                log.info("Created bucket: {}", bucketName);
            }

            return minioClient;
        } catch (Exception e) {
            log.error("MinIO client initialization failed. Endpoint: {}, AccessKey: {}", 
                    endpoint, accessKey != null ? "configured" : "not configured", e);
            throw new RuntimeException("MinIO client initialization failed", e);
        }
    }

    public String getFileUrl(String fileName) {
        return String.format("http://%s/%s/%s", endpoint, bucketName, fileName);
    }

    public String getPreviewUrl(String fileName) {
        try {
            // 构建Redis缓存key
            String cacheKey = "minio:preview:" + fileName;
            
            // 先从Redis中查询
            Object cachedUrl = redisTemplate.opsForValue().get(cacheKey);
            if (cachedUrl != null) {
                log.info("从Redis中获取预览URL: {}", cachedUrl);
                return (String) cachedUrl;
            }

            // 如果Redis中不存在，则从MinIO获取
            MinioClient client = minioClient();
            String previewUrl = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .method(io.minio.http.Method.GET)
                    .expiry(60, TimeUnit.MINUTES)
                    .build());

            // 只返回文件名，不包含域名/IP和端口信息
            String fileUrlWithoutDomain =bucketName + "/" + previewUrl.substring(previewUrl.indexOf(bucketName) + bucketName.length() + 1);

            // 将URL存储到Redis中，设置60分钟过期时间
            redisTemplate.opsForValue().set(cacheKey, fileUrlWithoutDomain, 60, TimeUnit.MINUTES);

            return fileUrlWithoutDomain;
        } catch (Exception e) {
            log.error("获取预览地址失败", e);
            throw new RuntimeException("获取预览地址失败", e);
        }
    }
} 