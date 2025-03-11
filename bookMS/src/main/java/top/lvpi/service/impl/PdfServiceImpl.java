package top.lvpi.service.impl;

import top.lvpi.common.BusinessException;
import top.lvpi.common.ErrorCode;
import top.lvpi.model.dto.book.BookUpdateRequest;
import top.lvpi.model.entity.Book;
import top.lvpi.model.entity.BookSection;
import top.lvpi.service.BookService;
import top.lvpi.service.BookSectionService;
import top.lvpi.service.FileService;
import top.lvpi.service.PdfService;
import top.lvpi.utils.PDFUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@Slf4j
public class PdfServiceImpl implements PdfService {

    @Autowired
    private FileService fileService;

    @Autowired
    private BookService bookService;

    @Autowired
    private BookSectionService bookSectionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String extractText(String fileName, Long bookId, String title) {
        if (StringUtils.isBlank(fileName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }
        if (bookId == null || bookId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图书ID不合法");
        }

        try {
            // 获取PDF文件输入流
            InputStream pdfStream = fileService.getFileInputStream(fileName);
            
            // 提取文本内容
            List<BookSection> sections = PDFUtils.extractTextToSections(pdfStream, bookId, title);
            
            // 保存章节信息
            if (sections != null && !sections.isEmpty()) {
                for (BookSection section : sections) {
                    bookSectionService.save(section);
                }
            }

            // 更新图书信息
            Book book = bookService.getBookById(bookId);
            if (book != null) {
                BookUpdateRequest updateRequest = new BookUpdateRequest();
                updateRequest.setId(bookId);
                
                // 更新页数
                try {
                    int pageCount = PDFUtils.getPageCount(fileService.getFileInputStream(fileName));
                    updateRequest.setPageSize(pageCount);
                } catch (Exception e) {
                    log.error("获取PDF页数失败", e);
                }
                
                // 更新文本提取状态
                updateRequest.setIsExtracted(1);
                
                // 保存更新
                bookService.updateBook(updateRequest);
            }

            pdfStream.close();
            return "文本提取成功，共提取 " + sections.size() + " 个章节";
        } catch (IOException e) {
            log.error("提取PDF文本失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "提取PDF文本失败：" + e.getMessage());
        }
    }
} 