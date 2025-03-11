package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import top.lvpi.common.BusinessException;
import top.lvpi.common.ErrorCode;
import top.lvpi.config.MinioConfig;
import top.lvpi.model.dto.file.BookFileDTO;
import top.lvpi.model.dto.file.FileUploadResult;
import top.lvpi.model.entity.Book;
import top.lvpi.model.entity.LpFile;
import top.lvpi.service.FileService;
import top.lvpi.service.LpFileService;
import top.lvpi.service.BookFileService;
import top.lvpi.service.BookService;
import top.lvpi.utils.FileUtils;
import io.minio.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private BookService bookService;

    @Autowired
    private LpFileService lpFileService;

    @Autowired
    private BookFileService bookFileService;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 计算文件的MD5值
     * @param inputStream 文件输入流
     * @return MD5值
     */
    private String calculateMD5(InputStream inputStream) throws IOException {
        try {
            return DigestUtils.md5Hex(inputStream);
        } finally {
            inputStream.close();
        }
    }

    /**
     * 根据MD5值查找已存在的文件
     * @param md5 文件的MD5值
     * @return 如果找到，返回文件名；否则返回null
     */
    private String findExistingFile(String md5) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix("")
                    .recursive(true)
                    .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                Map<String, String> userMetadata = minioClient.statObject(
                    StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(item.objectName())
                        .build()
                ).userMetadata();
                
                if (userMetadata != null && md5.equals(userMetadata.get("md5"))) {
                    return item.objectName();
                }
            }
        } catch (Exception e) {
            log.error("查找已存在文件失败", e);
        }
        return null;
    }

    /**
     * 获取重复文件对应的图书信息
     * @param fileName 文件名
     * @return 图书信息，如果未找到返回null
     */
    private Book getDuplicateBookInfo(String fileName) {
        try {
            //根据文件名获取文件信息
            LpFile lpFile = lpFileService.getOne(new LambdaQueryWrapper<LpFile>().eq(LpFile::getFileName, fileName));
            if (lpFile == null) {
                return null;
            }   
            //根据文件id获取图书id
            Long  fileId = lpFile.getFileId();
            BookFileDTO lpBookFileDTO = bookFileService.getBookFilesNoDeleteByBookId(fileId);
            if (lpBookFileDTO == null) {
                return null;
            }
            Book lpBook = bookService.getById(lpBookFileDTO.getBookId());
            if (lpBook == null) {
                return null;
            }
            return lpBook;
        } catch (Exception e) {
            log.error("查询重复文件图书信息失败", e);
            return null;
        }
    }
    @Override
    public FileUploadResult uploadFile(MultipartFile file) {
        try {
            // 计算文件MD5
            String md5 = calculateMD5(file.getInputStream());
            log.info("文件MD5: {}", md5);

            // 查找是否已存在相同文件
            String existingFileName = findExistingFile(md5);
            if (existingFileName != null) {
                log.info("找到已存在的文件: {}", existingFileName);
                // 获取重复文件对应的图书信息
                Book duplicateBook = getDuplicateBookInfo(existingFileName);
                if (duplicateBook != null) {
                    log.info("找到重复文件对应的图书: ID={}, 标题={}", duplicateBook.getId(), duplicateBook.getTitle());
                    // 如果是PDF文件，获取页数
                    int pageCount = 0;
                    if (existingFileName.toLowerCase().endsWith(".pdf")) {
                        try (InputStream inputStream = getFileInputStream(existingFileName);
                             PDDocument document = PDDocument.load(inputStream)) {
                            pageCount = document.getNumberOfPages();
                        }
                    }
                    return new FileUploadResult(existingFileName, pageCount, 
                        duplicateBook.getId(), duplicateBook.getTitle(), duplicateBook.getPicUrl());
                }
            }

            // 如果文件不存在，则上传新文件
            String extension = FileUtils.getFileExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString() + extension;
            
            // 如果是PDF文件，获取页数
            int pageCount = 0;
            if ("application/pdf".equals(file.getContentType())) {
                try (InputStream inputStream = file.getInputStream();
                     PDDocument document = PDDocument.load(inputStream)) {
                    pageCount = document.getNumberOfPages();
                }
            }
            
            // 设置自定义元数据
            Map<String, String> userMetadata = new HashMap<>();
            userMetadata.put("md5", md5);
            
            // 上传文件到MinIO
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .userMetadata(userMetadata)
                .build());
            
            log.info("文件上传成功: {}", fileName);
            return new FileUploadResult(fileName, pageCount);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }

    @Override
    public FileUploadResult uploadFile(File file) {
        try {
            // 计算文件MD5
            String md5;
            try (FileInputStream fis = new FileInputStream(file)) {
                md5 = calculateMD5(fis);
            }
            log.info("文件MD5: {}", md5);

            // 查找是否已存在相同文件
            String existingFileName = findExistingFile(md5);
            if (existingFileName != null) {
                log.info("找到已存在的文件: {}", existingFileName);
                // 获取重复文件对应的图书信息
                Book duplicateBook = getDuplicateBookInfo(existingFileName);
                if (duplicateBook != null) {
                    log.info("找到重复文件对应的图书: ID={}, 标题={}", duplicateBook.getId(), duplicateBook.getTitle());
                    // 如果是PDF文件，获取页数
                    int pageCount = 0;
                    if (existingFileName.toLowerCase().endsWith(".pdf")) {
                        try (InputStream inputStream = getFileInputStream(existingFileName);
                             PDDocument document = PDDocument.load(inputStream)) {
                            pageCount = document.getNumberOfPages();
                        }
                    }
                    return new FileUploadResult(existingFileName, pageCount, 
                        duplicateBook.getId(), duplicateBook.getTitle(), duplicateBook.getPicUrl());
                }
            }

            // 如果文件不存在，则上传新文件
            String extension = FileUtils.getFileExtension(file.getName());
            String fileName = UUID.randomUUID().toString() + extension;
            
            // 如果是PDF文件，获取页数
            int pageCount = 0;
            if (file.getName().toLowerCase().endsWith(".pdf")) {
                try (FileInputStream fis = new FileInputStream(file);
                     PDDocument document = PDDocument.load(fis)) {
                    pageCount = document.getNumberOfPages();
                }
            }
            
            // 设置自定义元数据
            Map<String, String> userMetadata = new HashMap<>();
            userMetadata.put("md5", md5);
            
            // 上传文件到MinIO
            minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .stream(new FileInputStream(file), file.length(), -1)
                .contentType(FileUtils.getContentType(file.getName()))
                .userMetadata(userMetadata)
                .build());
            
            log.info("文件上传成功: {}", fileName);
            return new FileUploadResult(fileName, pageCount);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
    }

    @Override
    public void downloadFile(String filePath, OutputStream outputStream) {
        try {
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(filePath)
                .build());
            
            IOUtils.copy(response, outputStream);
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败");
        }
    }
    @Override
    public String getPreviewUrl(String fileName) {
        return minioConfig.getPreviewUrl(fileName);
    }

    @Override
    public InputStream getFileInputStream(String fileName) throws IOException {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build()
            );
        } catch (Exception e) {
            log.error("获取文件输入流失败", e);
            throw new IOException("获取文件输入流失败: " + e.getMessage());
        }
    }
} 