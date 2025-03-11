package top.lvpi.model.dto.file;

import lombok.Data;

@Data
public class FileUploadResult {
    private String fileName;
    private int pageCount;
    private boolean isDuplicate;  // 是否是重复文件
    private Long duplicateDocId; // 重复文件对应的图书ID
    private String duplicateDocTitle; // 重复文件对应的图书标题
    private String picUrl; // 图书封面URL

    public FileUploadResult(String fileName, int pageCount) {
        this.fileName = fileName;
        this.pageCount = pageCount;
        this.isDuplicate = false;
    }

    public FileUploadResult(String fileName, int pageCount, Long duplicateDocId, String duplicateDocTitle, String picUrl) {
        this.fileName = fileName;
        this.pageCount = pageCount;
        this.isDuplicate = true;
        this.duplicateDocId = duplicateDocId;
        this.duplicateDocTitle = duplicateDocTitle;
        this.picUrl = picUrl;
    }
} 