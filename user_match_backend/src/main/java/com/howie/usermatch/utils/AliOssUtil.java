package com.howie.usermatch.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云 OSS 工具类：上传文件并返回可访问 URL。
 * <p>
 * AccessKey 从环境变量读取：OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET（RAM 账号）。
 * endpoint / bucketName 从 application.yml 读取（aliyun.oss.*）。
 */
@Component
public class AliOssUtil {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    /**
     * 上传文件到 OSS
     *
     * @param file 前端上传的文件
     * @return 文件可访问的 URL（https://{bucket}.{endpoint}/{objectName}）
     */
    public String upload(MultipartFile file) {
        // 1. 生成唯一文件名，避免同名覆盖
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = "avatar/" + UUID.randomUUID() + extension;

        // 2. 从环境变量读取 AccessKey（不硬编码在代码里），创建客户端
        OSS ossClient;
        try {
            EnvironmentVariableCredentialsProvider credentialsProvider =
                    CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
            ossClient = new OSSClientBuilder().build(endpoint, credentialsProvider);
        } catch (com.aliyuncs.exceptions.ClientException e) {
            throw new RuntimeException("读取 OSS 环境变量凭证失败，请检查 OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET", e);
        }

        // 3. 上传文件
        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(bucketName, objectName, inputStream);
        } catch (IOException e) {
            throw new RuntimeException("OSS 上传失败", e);
        } finally {
            ossClient.shutdown();
        }

        // 4. 返回可访问 URL（需 Bucket 为「公共读」权限，否则需改用签名 URL）
        return "https://" + bucketName + "." + endpoint + "/" + objectName;
    }
}
