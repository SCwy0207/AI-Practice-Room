package com.victor.ai_practice_room.service.impl;

import com.victor.ai_practice_room.properties.MinioProperties;
import com.victor.ai_practice_room.service.FileUploadService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

/**
 * projectName: com.victor.ai_practice_room.service.impl
 *
 * @author: 赵伟风
 * description:
 */
@Service
@Slf4j
@EnableConfigurationProperties(MinioProperties.class)
public class FileUploadServiceImpl implements FileUploadService {
    @Autowired
    private MinioProperties minioProperties;
    @Override
    public String uploadFile(MultipartFile file) {
//        RLock lock = redisson.getLock("minio:bucket:create:" + minioProperties.getBucketName())
        String newFileName = null;
        try {
            //创建MinioClient对象
            MinioClient minioClient = MinioClient.builder()
                    .endpoint(minioProperties.getEndPoint())
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                    .build();
            //判断桶是否存在，不存在则新建
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
            }
            /*
             * 上传本地文件的时候使用UploadObjectArgs对象
             * 通过输入流上传文件使用PutObjectArgs对象
             * */
            String uploadFileName = file.getOriginalFilename();
            String prefix = UUID.randomUUID().toString().replaceAll("-", "");
            String suffix = uploadFileName.substring(uploadFileName.lastIndexOf("."));
            String dateFormat = DateFormatUtils.format(new Date(), "yyyy/MM/dd");
            //拼接新的文件名
            newFileName = dateFormat + "/" + prefix + suffix;
            //获取输入流
            InputStream inputStream = file.getInputStream();
            //获取上传文件的大小
            long fileSize = file.getSize();
            //创建PutObjectArgs对象
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(newFileName)
                    //指定输入流，“-1”表示使用PutObjectArgs的默认分片大小
                    .stream(inputStream, fileSize, -1)
                    .build();
            //上传到minio服务器
            minioClient.putObject(putObjectArgs);
            log.info(newFileName + "文件上传成功");
            return minioProperties.getEndPoint()+"/"+minioProperties.getBucketName()+"/"+newFileName;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(newFileName + "文件上传失败");
        }
        return null;
    }
}
