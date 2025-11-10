package com.victor.ai_practice_room.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endPoint;
    private String bucketName;
    private String accessKey;
    private String secretKey;
}
