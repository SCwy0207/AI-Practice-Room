package com.victor.ai_practice_room.service;


import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务
 * 支持MinIO和本地文件存储两种方式
 */

public interface FileUploadService {

    String uploadFile(MultipartFile file);
}
