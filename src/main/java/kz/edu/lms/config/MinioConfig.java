package kz.edu.lms.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    @Bean
    public MinioClient minioClient(@Value("${minio.endpoint}") String endpoint,
                                   @Value("${minio.access-key}") String accessKey,
                                   @Value("${minio.secret-key}") String secretKey) {
        var client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        client.setTimeout(5000, 60000, 60000);
        return client;
    }
}
