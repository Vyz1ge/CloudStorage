package com.visage.cloudstorage.integration.Controllers;

import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


@Configuration
public class ConfigMinio {

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder()
                .endpoint("http://localhost:9000")
                .credentials("minioadmin", "minioadmin")
                .build();
    }

}
