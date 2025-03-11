package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.lvpi.common.BusinessException;
import top.lvpi.common.ErrorCode;
import top.lvpi.mapper.NoteMapper;
import top.lvpi.mapper.NoteReportRelateMapper;
import top.lvpi.mapper.SearchReportMapper;
import top.lvpi.model.entity.Note;
import top.lvpi.model.entity.NoteReportRelate;
import top.lvpi.model.entity.SearchReport;
import top.lvpi.service.SearchReportService;
import top.lvpi.utils.MinioUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchReportServiceImpl extends ServiceImpl<SearchReportMapper, SearchReport> implements SearchReportService {

    @Autowired
    private NoteReportRelateMapper noteReportRelateMapper;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private MinioUtils minioUtils;

    private static final String SHARE_PREFIX = "share/reports/";
    private static final long SHARE_EXPIRY_HOURS = 24; // 分享链接24小时后过期

    @Override
    public boolean createReport(SearchReport report) {
        if (report == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isBlank(report.getTitle())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "报告标题不能为空");
        }
        report.setCreateTime(LocalDateTime.now());
        report.setModifiedTime(LocalDateTime.now());
        return save(report);
    }

    @Override
    public boolean updateReport(SearchReport report) {
        if (report == null || report.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SearchReport existReport = getById(report.getId());
        if (existReport == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        report.setModifiedTime(LocalDateTime.now());
        return updateById(report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteReport(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        SearchReport report = getById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 删除关联关系
        LambdaQueryWrapper<NoteReportRelate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NoteReportRelate::getReportId, id);
        noteReportRelateMapper.delete(queryWrapper);
        // 删除报告
        return removeById(id);
    }

    @Override
    public SearchReport getReportById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return getById(id);
    }

    @Override
    public IPage<SearchReport> listReports(int current, int size, String keyword, Long userId) {
        Page<SearchReport> page = new Page<>(current, size);
        LambdaQueryWrapper<SearchReport> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加用户ID过滤
        queryWrapper.eq(SearchReport::getUserId, userId);
        
        // 添加关键词搜索条件
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.like(SearchReport::getTitle, keyword)
                    .or()
                    .like(SearchReport::getType, keyword)
                    .and(wrapper -> wrapper.eq(SearchReport::getUserId, userId));
        }
        
        queryWrapper.orderByDesc(SearchReport::getCreateTime);
        return page(page, queryWrapper);
    }

    @Override
    public List<Note> getReportNotes(Long reportId) {
        if (reportId == null || reportId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取关联关系
        LambdaQueryWrapper<NoteReportRelate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NoteReportRelate::getReportId, reportId);
        List<NoteReportRelate> relates = noteReportRelateMapper.selectList(queryWrapper);
        
        // 获取笔记列表
        List<Long> noteIds = relates.stream()
                .map(NoteReportRelate::getNoteId)
                .collect(Collectors.toList());
        if (noteIds.isEmpty()) {
            return List.of();
        }
        return noteMapper.selectBatchIds(noteIds);
    }

    @Override
    public byte[] exportReportToPdf(Long reportId) {
        SearchReport report = getReportById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        List<Note> notes = getReportNotes(reportId);

        try (PDDocument document = new PDDocument()) {
            // 尝试加载中文字体，如果失败则使用备选字体
            PDFont font;
            try {
                font = PDType0Font.load(document, new ClassPathResource("fonts/simsun.ttc").getInputStream());
            } catch (IOException e1) {
                log.warn("无法加载simsun.ttc字体，尝试加载备选字体: {}", e1.getMessage());
                try {
                    font = PDType0Font.load(document, new ClassPathResource("fonts/simsun.ttf").getInputStream());
                } catch (IOException e2) {
                    log.warn("无法加载simsun.ttf字体，使用标准字体: {}", e2.getMessage());
                    font = PDType1Font.HELVETICA;
                }
            }
            
            PDPage currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);
            float margin = 50;
            float yStart = currentPage.getMediaBox().getHeight() - margin;
            float fontSize = 12;
            float leading = 1.5f * fontSize;
            float y = yStart;
            
            // 写入标题和基本信息
            try (PDPageContentStream contentStream = new PDPageContentStream(document, currentPage)) {
                // 写入标题
                contentStream.beginText();
                contentStream.setFont(font, 16);
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText(report.getTitle());
                contentStream.endText();
                
                // 写入报告信息
                y -= 2 * leading;
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("报告类型：" + report.getType());
                contentStream.newLineAtOffset(0, -leading);
                contentStream.showText("创建时间：" + formatDateTime(report.getCreateTime()));
                contentStream.newLineAtOffset(0, -leading);
                contentStream.showText("修改时间：" + formatDateTime(report.getModifiedTime()));
                contentStream.endText();
                
                y -= 4 * leading;
            }
            
            // 写入笔记列表
            for (int i = 0; i < notes.size(); i++) {
                Note note = notes.get(i);
                
                // 如果当前页空间不足，创建新页
                if (y < margin + 4 * leading) {
                    currentPage = new PDPage(PDRectangle.A4);
                    document.addPage(currentPage);
                    y = yStart;
                }
                
                PDPageContentStream contentStream = new PDPageContentStream(document, currentPage, PDPageContentStream.AppendMode.APPEND, true);
                try {
                    // 写入笔记标题
                    contentStream.beginText();
                    contentStream.setFont(font, 14);
                    contentStream.newLineAtOffset(margin, y);
                    contentStream.showText((i + 1) + ". " + (StringUtils.isNotBlank(note.getSourceName()) ? note.getSourceName() : "无标题笔记"));
                    contentStream.endText();
                    y -= 1.5f * leading;
                    
                    // 写入笔记内容
                    String content = note.getContent();
                    List<String> lines = splitTextIntoLines(content, font, fontSize, currentPage.getMediaBox().getWidth() - 2 * margin);
                    
                    contentStream.beginText();
                    contentStream.setFont(font, fontSize);
                    contentStream.newLineAtOffset(margin, y);
                    
                    for (String line : lines) {
                        // 如果当前页空间不足，创建新页
                        if (y < margin + leading) {
                            contentStream.endText();
                            contentStream.close();
                            
                            currentPage = new PDPage(PDRectangle.A4);
                            document.addPage(currentPage);
                            y = yStart;
                            
                            contentStream = new PDPageContentStream(document, currentPage, PDPageContentStream.AppendMode.APPEND, true);
                            contentStream.beginText();
                            contentStream.setFont(font, fontSize);
                            contentStream.newLineAtOffset(margin, y);
                            contentStream.showText(line);
                            contentStream.endText();
                            y -= leading;
                            continue;
                        }
                        
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(0, -leading);
                        y -= leading;
                    }
                    contentStream.endText();
                    
                    // 如果有标签，写入标签信息
                    if (StringUtils.isNotBlank(note.getTags())) {
                        y -= leading;
                        contentStream.beginText();
                        contentStream.setFont(font, fontSize);
                        contentStream.newLineAtOffset(margin, y);
                        contentStream.showText("标签：" + note.getTags());
                        contentStream.endText();
                    }
                    
                    y -= 2 * leading;
                } finally {
                    contentStream.close();
                }
            }
            
            // 将文档写入字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
            
        } catch (IOException e) {
            log.error("生成PDF报告失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成PDF报告失败");
        }
    }
    
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    private List<String> splitTextIntoLines(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        String[] words = text.split("(?<=\\s)|(?=\\s)");
        
        for (String word : words) {
            float currentWidth = font.getStringWidth(currentLine + word) * fontSize / 1000;
            
            if (currentWidth > maxWidth) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
                // 如果单个词太长，需要强制换行
                if (font.getStringWidth(word) * fontSize / 1000 > maxWidth) {
                    for (int i = 0; i < word.length(); i++) {
                        String subWord = word.substring(0, i + 1);
                        if (font.getStringWidth(subWord) * fontSize / 1000 > maxWidth) {
                            if (i > 0) {
                                lines.add(word.substring(0, i));
                                word = word.substring(i);
                                i = 0;
                            }
                        }
                    }
                    if (!word.isEmpty()) {
                        currentLine.append(word);
                    }
                } else {
                    currentLine.append(word);
                }
            } else {
                currentLine.append(word);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }

    @Override
    public String exportReportToMarkdown(Long reportId) {
        SearchReport report = getReportById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        List<Note> notes = getReportNotes(reportId);
        
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(report.getTitle()).append("\n\n");
        markdown.append("## 报告信息\n");
        markdown.append("- 类型：").append(report.getType()).append("\n");
        markdown.append("- 创建时间：").append(report.getCreateTime()).append("\n");
        markdown.append("- 修改时间：").append(report.getModifiedTime()).append("\n\n");
        
        markdown.append("## 笔记列表\n");
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            markdown.append("### ").append(i + 1).append(". ");
            if (StringUtils.isNotBlank(note.getSourceName())) {
                markdown.append(note.getSourceName());
            }
            markdown.append("\n");
            markdown.append(note.getContent()).append("\n\n");
            if (StringUtils.isNotBlank(note.getTags())) {
                markdown.append("标签：").append(note.getTags()).append("\n");
            }
            markdown.append("---\n");
        }
        
        return markdown.toString();
    }

    @Override
    public String generateShareLink(Long reportId) {
        SearchReport report = getReportById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        try {
            // 生成PDF文件
            byte[] pdfData = exportReportToPdf(reportId);
            
            // 生成唯一的文件名
            String fileName = SHARE_PREFIX + UUID.randomUUID().toString() + ".pdf";
            
            // 上传到MinIO，设置过期时间
            Map<String, String> headers = new HashMap<>();
            headers.put("expires", String.valueOf(System.currentTimeMillis() + SHARE_EXPIRY_HOURS * 3600 * 1000));
            
            minioUtils.uploadTempFile(fileName, pdfData, headers);
            
            // 获取MinIO文件下载地址
            String downloadUrl = minioUtils.getFileDownloadUrl(fileName);
            if (downloadUrl == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取文件下载地址失败");
            }
            
            return downloadUrl;
        } catch (Exception e) {
            log.error("生成分享链接失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成分享链接失败");
        }
    }

    @Override
    public byte[] getSharedReport(String fileName) {
        if (!fileName.startsWith(SHARE_PREFIX)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的分享链接");
        }

        try {
            // 获取文件元数据
            Map<String, String> metadata = minioUtils.getFileMetadata(fileName);
            if (metadata == null || !metadata.containsKey("expires")) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分享链接不存在或已过期");
            }

            // 检查是否过期
            long expiryTime = Long.parseLong(metadata.get("expires"));
            if (System.currentTimeMillis() > expiryTime) {
                // 删除过期文件
                minioUtils.deleteFile(fileName);
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分享链接已过期");
            }

            // 获取文件内容
            return minioUtils.getFileBytes(fileName);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取分享报告失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取分享报告失败");
        }
    }

    @Override
    public List<SearchReport> getUserReports(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<SearchReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SearchReport::getUserId, userId)
                .orderByDesc(SearchReport::getCreateTime);
        return list(queryWrapper);
    }

    @Override
    public List<SearchReport> getSearchSubjectReports(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<SearchReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SearchReport::getUserId, userId)
                .eq(SearchReport::getSearchSubject, 1)
                .orderByDesc(SearchReport::getCreateTime);
        return list(queryWrapper);
    }

    private String convertToAscii(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c < 128) {
                result.append(c);
            } else {
                result.append("[").append((int)c).append("]");
            }
        }
        return result.toString();
    }
} 