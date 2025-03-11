package top.lvpi.service;

import top.lvpi.model.dto.file.FileUploadResult;
import org.springframework.web.multipart.MultipartFile;
import java.io.OutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;

public interface FileService {
    /**
     * 上传文件并返回文件信息
     */
    FileUploadResult uploadFile(MultipartFile file);

    /**
     * 上传File类型文件并返回文件信息
     */
    FileUploadResult uploadFile(File file);

    /**
     * 下载文件
     */
    void downloadFile(String filePath, OutputStream outputStream);

    /**
     * 获取文件预览地址
     */
    String getPreviewUrl(String fileName);

    /**
     * 获取文件输入流
     * @param fileName 文件名
     * @return 文件输入流
     * @throws IOException IO异常
     */
    InputStream getFileInputStream(String fileName) throws IOException;
} 