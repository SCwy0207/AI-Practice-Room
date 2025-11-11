package com.victor.ai_practice_room.utils;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class FileUploader {

    public static void main(String[] args)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        try {
            // Create a minioClient with the MinIO server playground, its access key and secret key.
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint("http://192.168.6.100:9000")
                            .credentials("minioadmin", "minioadmin")
                            .build();

            // Make 'ai-practice-room' bucket if not exist.
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket("ai-practice-room").build());
            if (!found) {
                // Make a new bucket called 'ai-practice-room'.
                minioClient.makeBucket(MakeBucketArgs.builder().bucket("ai-practice-room").build());
            } else {
                System.out.println("Bucket 'ai-practice-room' already exists.");
            }

            // Upload 'fileRealPath as object name objectName to bucket
            String objectName = UUID.randomUUID().toString()+".png";
            String fileRealPath = "D:\\Alex\\Desktop\\Image_20251107164024_87_82.png";
            // 'ai-practice-room'.
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket("ai-practice-room")
                            .object(objectName)
                            .filename(fileRealPath)
                            .build());
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
            System.out.println("HTTP trace: " + e.httpTrace());
        }
    }
}