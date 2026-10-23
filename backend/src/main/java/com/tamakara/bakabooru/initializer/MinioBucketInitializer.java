package com.tamakara.bakabooru.initializer;

import com.tamakara.bakabooru.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioBucketInitializer implements ApplicationRunner {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String bucket = minioConfig.getBucketName();
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("已创建 MinIO bucket: {}", bucket);
        }

        String publicReadPolicy = """
                {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},"Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}
                """.formatted(bucket);
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder().bucket(bucket).config(publicReadPolicy).build()
        );
        log.info("已确保 MinIO bucket 允许公开读取: {}", bucket);
    }
}
