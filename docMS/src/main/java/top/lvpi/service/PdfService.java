package top.lvpi.service;

public interface PdfService {
    /**
     * 提取PDF文本并保存到数据库
     * @param filePath PDF文件路径
     * @param docId 书籍ID
     * @return 处理结果
     */
    String extractText(String filePath, Long docId, String title);
} 