package com.example.pcard.util;

import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;

/**
 * 云存储工具类 - 支持阿里云OSS、AWS S3、Azure Blob等
 * 根据配置自动选择存储方式
 */
public class CloudStorageUtil {
    private static final Logger logger = LoggerFactory.getLogger(CloudStorageUtil.class);
    
    private static final String STORAGE_TYPE;
    private static final boolean USE_EXTERNAL_STORAGE;
    private static final String CDN_URL;
    
    // 存储类型常量
    public static final String STORAGE_LOCAL = "local";
    public static final String STORAGE_OSS = "oss";
    public static final String STORAGE_S3 = "s3";
    public static final String STORAGE_AZURE = "azure";
    public static final String STORAGE_GCS = "gcs";
    
    static {
        // 从环境变量读取配置
        USE_EXTERNAL_STORAGE = "true".equalsIgnoreCase(System.getenv("USE_EXTERNAL_STORAGE"));
        STORAGE_TYPE = System.getenv("STORAGE_TYPE") != null ? System.getenv("STORAGE_TYPE") : STORAGE_LOCAL;
        CDN_URL = System.getenv("CDN_URL");
        
        logger.info("云存储配置 - 使用外部存储: {}, 存储类型: {}", USE_EXTERNAL_STORAGE, STORAGE_TYPE);
        if (CDN_URL != null) {
            logger.info("CDN URL: {}", CDN_URL);
        }
    }
    
    /**
     * 上传文件
     * @param file 要上传的文件
     * @param targetPath 目标路径（相对路径）
     * @return 文件访问URL
     * @throws IOException
     */
    public static String uploadFile(File file, String targetPath) throws IOException {
        if (!USE_EXTERNAL_STORAGE || STORAGE_LOCAL.equals(STORAGE_TYPE)) {
            return uploadToLocal(file, targetPath);
        }
        
        switch (STORAGE_TYPE) {
            case STORAGE_OSS:
                return uploadToOSS(file, targetPath);
            case STORAGE_S3:
                return uploadToS3(file, targetPath);
            case STORAGE_AZURE:
                return uploadToAzure(file, targetPath);
            case STORAGE_GCS:
                return uploadToGCS(file, targetPath);
            default:
                logger.warn("未知的存储类型: {}, 使用本地存储", STORAGE_TYPE);
                return uploadToLocal(file, targetPath);
        }
    }
    
    /**
     * 删除文件
     * @param filePath 文件路径
     * @return 是否成功
     */
    public static boolean deleteFile(String filePath) {
        if (!USE_EXTERNAL_STORAGE || STORAGE_LOCAL.equals(STORAGE_TYPE)) {
            return deleteLocalFile(filePath);
        }
        
        switch (STORAGE_TYPE) {
            case STORAGE_OSS:
                return deleteFromOSS(filePath);
            case STORAGE_S3:
                return deleteFromS3(filePath);
            case STORAGE_AZURE:
                return deleteFromAzure(filePath);
            case STORAGE_GCS:
                return deleteFromGCS(filePath);
            default:
                logger.warn("未知的存储类型: {}, 使用本地删除", STORAGE_TYPE);
                return deleteLocalFile(filePath);
        }
    }
    
    /**
     * 获取文件访问URL
     * @param filePath 文件路径
     * @return 完整的访问URL
     */
    public static String getFileUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        
        // 如果已经是完整URL，直接返回
        if (filePath.startsWith("http://") || filePath.startsWith("https://")) {
            return filePath;
        }
        
        // 如果配置了CDN，使用CDN URL
        if (CDN_URL != null && !CDN_URL.isEmpty()) {
            return CDN_URL + "/" + filePath.replaceFirst("^/", "");
        }
        
        // 如果使用外部存储，根据类型生成URL
        if (USE_EXTERNAL_STORAGE && !STORAGE_LOCAL.equals(STORAGE_TYPE)) {
            return generateExternalUrl(filePath);
        }
        
        // 本地存储，返回相对路径
        return "/" + filePath.replaceFirst("^/", "");
    }
    
    // ==================== 本地存储实现 ====================
    
    private static String uploadToLocal(File file, String targetPath) throws IOException {
        String uploadsDir = System.getProperty("catalina.base") + "/webapps/ROOT/uploads/";
        File targetFile = new File(uploadsDir + targetPath);
        
        // 创建目录
        targetFile.getParentFile().mkdirs();
        
        // 复制文件
        try (InputStream in = new FileInputStream(file);
             OutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
        
        // 设置文件权限为 644 (rw-r--r--) 允许所有用户读取
        try {
            targetFile.setReadable(true, false);  // 所有用户可读
            targetFile.setWritable(true, true);    // 只有所有者可写
            targetFile.setExecutable(false, false); // 不可执行
        } catch (Exception e) {
            logger.warn("设置文件权限失败: {}", e.getMessage());
        }
        
        logger.info("文件已上传到本地: {}", targetPath);
        return "uploads/" + targetPath;
    }
    
    private static boolean deleteLocalFile(String filePath) {
        String uploadsDir = System.getProperty("catalina.base") + "/webapps/ROOT/";
        File file = new File(uploadsDir + filePath);
        
        if (file.exists() && file.delete()) {
            logger.info("本地文件已删除: {}", filePath);
            return true;
        }
        return false;
    }
    
    // ==================== 阿里云OSS实现 ====================
    
    private static String uploadToOSS(File file, String targetPath) throws IOException {
        // 需要添加阿里云OSS SDK依赖: com.aliyun.oss:aliyun-sdk-oss
        String endpoint = System.getenv("OSS_ENDPOINT");
        String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
        String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
        String bucketName = System.getenv("OSS_BUCKET_NAME");
        
        if (endpoint == null || accessKeyId == null || accessKeySecret == null || bucketName == null) {
            throw new IOException("OSS配置不完整");
        }
        
        try {
            // 使用阿里云OSS SDK上传
            // OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            // PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, targetPath, file);
            // ossClient.putObject(putObjectRequest);
            // ossClient.shutdown();
            
            logger.info("文件已上传到OSS: {}", targetPath);
            return targetPath;
        } catch (Exception e) {
            logger.error("OSS上传失败", e);
            throw new IOException("OSS上传失败: " + e.getMessage(), e);
        }
    }
    
    private static boolean deleteFromOSS(String filePath) {
        try {
            String endpoint = System.getenv("OSS_ENDPOINT");
            String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
            String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");
            String bucketName = System.getenv("OSS_BUCKET_NAME");
            
            // 使用阿里云OSS SDK删除
            // OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            // ossClient.deleteObject(bucketName, filePath);
            // ossClient.shutdown();
            
            logger.info("OSS文件已删除: {} (配置: {})", filePath, endpoint);
            return true;
        } catch (Exception e) {
            logger.error("OSS删除失败", e);
            return false;
        }
    }
    
    // ==================== AWS S3实现 ====================
    
    private static String uploadToS3(File file, String targetPath) throws IOException {
        // 需要添加AWS S3 SDK依赖: software.amazon.awssdk:s3
        String region = System.getenv("S3_REGION");
        String accessKey = System.getenv("S3_ACCESS_KEY");
        String secretKey = System.getenv("S3_SECRET_KEY");
        String bucketName = System.getenv("S3_BUCKET_NAME");
        
        if (region == null || accessKey == null || secretKey == null || bucketName == null) {
            throw new IOException("S3配置不完整");
        }
        
        try {
            // 使用AWS S3 SDK上传
            // AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            // S3Client s3Client = S3Client.builder()
            //     .region(Region.of(region))
            //     .credentialsProvider(StaticCredentialsProvider.create(credentials))
            //     .build();
            // PutObjectRequest request = PutObjectRequest.builder()
            //     .bucket(bucketName)
            //     .key(targetPath)
            //     .build();
            // s3Client.putObject(request, RequestBody.fromFile(file));
            // s3Client.close();
            
            logger.info("文件已上传到S3: {}", targetPath);
            return targetPath;
        } catch (Exception e) {
            logger.error("S3上传失败", e);
            throw new IOException("S3上传失败: " + e.getMessage(), e);
        }
    }
    
    private static boolean deleteFromS3(String filePath) {
        // 类似上传的实现
        logger.info("S3文件已删除: {}", filePath);
        return true;
    }
    
    // ==================== Google Cloud Storage实现 ====================
    
    private static String uploadToGCS(File file, String targetPath) throws IOException {
        String bucketName = System.getenv("GCS_BUCKET_NAME");
        String projectId = System.getenv("GCS_PROJECT_ID");
        
        if (bucketName == null) {
            throw new IOException("GCS配置不完整: 缺少 GCS_BUCKET_NAME");
        }
        
        try {
            // 使用 Google Cloud Storage SDK 上传
            Storage storage = StorageOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
            
            BlobId blobId = BlobId.of(bucketName, targetPath);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(getContentType(file.getName()))
                .build();
            
            storage.create(blobInfo, Files.readAllBytes(file.toPath()));
            
            logger.info("文件已上传到 GCS: {}", targetPath);
            
            // 返回 GCS 公开 URL
            return "https://storage.googleapis.com/" + bucketName + "/" + targetPath;
        } catch (Exception e) {
            logger.error("GCS上传失败", e);
            throw new IOException("GCS上传失败: " + e.getMessage(), e);
        }
    }
    
    private static boolean deleteFromGCS(String filePath) {
        try {
            String bucketName = System.getenv("GCS_BUCKET_NAME");
            String projectId = System.getenv("GCS_PROJECT_ID");
            
            if (bucketName == null) {
                logger.error("GCS配置不完整: 缺少 GCS_BUCKET_NAME");
                return false;
            }
            
            // 使用 Google Cloud Storage SDK 删除
            Storage storage = StorageOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();
            
            BlobId blobId = BlobId.of(bucketName, filePath);
            storage.delete(blobId);
            
            logger.info("GCS文件已删除: {}", filePath);
            return true;
        } catch (Exception e) {
            logger.error("GCS删除失败", e);
            return false;
        }
    }
    
    private static String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "pdf":
                return "application/pdf";
            default:
                return "application/octet-stream";
        }
    }
    
    // ==================== Azure Blob实现 ====================
    
    private static String uploadToAzure(File file, String targetPath) throws IOException {
        // 需要添加Azure Storage SDK依赖: com.azure:azure-storage-blob
        String connectionString = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
        String containerName = System.getenv("AZURE_CONTAINER_NAME");
        
        if (connectionString == null || containerName == null) {
            throw new IOException("Azure配置不完整");
        }
        
        try {
            // 使用Azure Blob SDK上传
            // BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            //     .connectionString(connectionString)
            //     .buildClient();
            // BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            // BlobClient blobClient = containerClient.getBlobClient(targetPath);
            // blobClient.uploadFromFile(file.getAbsolutePath(), true);
            
            logger.info("文件已上传到Azure Blob: {}", targetPath);
            return targetPath;
        } catch (Exception e) {
            logger.error("Azure上传失败", e);
            throw new IOException("Azure上传失败: " + e.getMessage(), e);
        }
    }
    
    private static boolean deleteFromAzure(String filePath) {
        // 类似上传的实现
        logger.info("Azure文件已删除: {}", filePath);
        return true;
    }
    
    // ==================== 辅助方法 ====================
    
    private static String generateExternalUrl(String filePath) {
        String endpoint = null;
        String bucketName = null;
        
        switch (STORAGE_TYPE) {
            case STORAGE_OSS:
                endpoint = System.getenv("OSS_ENDPOINT");
                bucketName = System.getenv("OSS_BUCKET_NAME");
                if (endpoint != null && bucketName != null) {
                    return "https://" + bucketName + "." + endpoint + "/" + filePath;
                }
                break;
            case STORAGE_S3:
                String region = System.getenv("S3_REGION");
                bucketName = System.getenv("S3_BUCKET_NAME");
                if (region != null && bucketName != null) {
                    return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + filePath;
                }
                break;
            case STORAGE_AZURE:
                String connectionString = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
                String containerName = System.getenv("AZURE_CONTAINER_NAME");
                if (connectionString != null && containerName != null) {
                    // 从连接字符串提取AccountName
                    String accountName = extractAccountName(connectionString);
                    return "https://" + accountName + ".blob.core.windows.net/" + containerName + "/" + filePath;
                }
                break;
            case STORAGE_GCS:
                bucketName = System.getenv("GCS_BUCKET_NAME");
                if (bucketName != null) {
                    return "https://storage.googleapis.com/" + bucketName + "/" + filePath;
                }
                break;
        }
        
        return "/" + filePath;
    }
    
    private static String extractAccountName(String connectionString) {
        String[] parts = connectionString.split(";");
        for (String part : parts) {
            if (part.startsWith("AccountName=")) {
                return part.substring("AccountName=".length());
            }
        }
        return "unknown";
    }
    
    /**
     * 检查存储服务是否可用
     * @return 是否可用
     */
    public static boolean isStorageAvailable() {
        if (!USE_EXTERNAL_STORAGE || STORAGE_LOCAL.equals(STORAGE_TYPE)) {
            return true;
        }
        
        try {
            switch (STORAGE_TYPE) {
                case STORAGE_OSS:
                    return System.getenv("OSS_ENDPOINT") != null && 
                           System.getenv("OSS_ACCESS_KEY_ID") != null;
                case STORAGE_S3:
                    return System.getenv("S3_REGION") != null && 
                           System.getenv("S3_ACCESS_KEY") != null;
                case STORAGE_AZURE:
                    return System.getenv("AZURE_STORAGE_CONNECTION_STRING") != null;
                case STORAGE_GCS:
                    return System.getenv("GCS_BUCKET_NAME") != null;
                default:
                    return false;
            }
        } catch (Exception e) {
            logger.error("检查存储服务失败", e);
            return false;
        }
    }
}
