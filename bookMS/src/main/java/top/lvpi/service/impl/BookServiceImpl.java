package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.lvpi.common.BusinessException;
import top.lvpi.common.ErrorCode;
import top.lvpi.mapper.BookMapper;
import top.lvpi.mapper.BookSectionMapper;
import top.lvpi.model.dto.book.BookAddRequest;
import top.lvpi.model.dto.book.BookQueryRequest;
import top.lvpi.model.dto.book.BookUpdateRequest;
import top.lvpi.model.dto.file.BookFileDTO;
import top.lvpi.model.dto.file.FileUploadResult;
import top.lvpi.model.entity.Book;
import top.lvpi.model.entity.BookFile;
import top.lvpi.model.entity.BookSection;
import top.lvpi.model.entity.BookTopic;
import top.lvpi.model.entity.LpFile;
import top.lvpi.model.entity.OpacBookInfo;
import top.lvpi.model.vo.BookVO;
import top.lvpi.service.BookFileService;
import top.lvpi.service.BookSectionEsService;
import top.lvpi.service.BookSectionService;
import top.lvpi.service.BookService;
import top.lvpi.service.BookTopicService;
import top.lvpi.service.ImgService;
import top.lvpi.service.LpFileService;
import top.lvpi.service.TopicService;
import top.lvpi.utils.BookUtils;
import top.lvpi.utils.MinioUtils;
    
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Autowired
    private MinioUtils minioUtils;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BookSectionMapper bookSectionMapper;

    @Autowired
    private BookUtils bookUtils;

    @Autowired
    private ImgService imgService;

    @Autowired
    private LpFileService lpFileService;

    @Autowired
    private BookFileService bookFileService;

    @Autowired
    private BookSectionEsService bookSectionEsService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private BookTopicService bookTopicService;

    @Autowired
    private BookSectionService bookSectionService;

    @Value("${file.upload-dir:/app/filedata}")
    private String uploadDir;

    @Override
    public boolean hasBookSections(Long bookId) {
        LambdaQueryWrapper<BookSection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BookSection::getBookId, bookId);
        return bookSectionMapper.selectCount(queryWrapper) > 0;
    }


    @Override
    public int addBook(BookAddRequest bookAddRequest) {
        LpBook book = new LpBook();
        BeanUtils.copyProperties(bookAddRequest, book);
        try {
            int result = bookMapper.insert(book);
            if (result <= 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增图书失败");
            }
            //判断是否存在文件id
            if (bookAddRequest.getFileId() != null) {
                //关联图书与文件
                LpBookFile lpBookFile = new LpBookFile();
                lpBookFile.setBookId(book.getId());
                lpBookFile.setFileId(bookAddRequest.getFileId());
                boolean save = lpBookFileService.save(lpBookFile);
                if (!save) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "关联图书与文件失败");
                }
            }
            return result;
        } catch (Exception e) {
            log.error("新增图书失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增图书失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBook(Long id) {
        try {
            //逻辑删除图书
            Book book = new Book();
            book.setId(id);
            book.setIsDeleted(1);
            boolean bookResult = this.updateById(book);
            if (!bookResult) {
                return false;
            }
            //检查是否存在关联图片
            Book bookData = this.getBookById(id);
            if (bookData.getPicUrl() != null) {
                Long imgId = Long.parseLong(bookData.getPicUrl().replace("img/", ""));
                if (imgId != null) {
                    boolean imgResult = imgService.removeById(imgId);
                    if (!imgResult) {
                        log.error("删除图书封面失败，imgId: " + imgId);
                        return false;
                    }
                }
            }

            //检查是否存在图书文件关联表，如存在则逻辑删除
            Long fileAssocCount = bookFileService.count(
                new LambdaQueryWrapper<BookFile>()
                    .eq(BookFile::getBookId, id)
                    .eq(BookFile::getIsDeleted, 0)
            );
            if (fileAssocCount > 0) {   
                //逻辑删除图书文件关联表
                BookFile bookFile = new BookFile();
                bookFile.setIsDeleted(1);
                boolean fileAssocResult = bookFileService.update(bookFile, 
                new LambdaUpdateWrapper<BookFile>().eq(BookFile::getBookId, id));
                if (!fileAssocResult) {
                    log.error("删除图书文件关联表失败，bookId: " + id);
                    return false;
                }
            } else {
                log.info("No book files found, skipping file deletion");
            }

            //检查是否存在文件表，如存在则逻辑删除
            BookFileDTO bookFileDTO = bookFileService.getBookFilesNoDeleteByBookId(id);
            if (bookFileDTO != null) {
                LpFile lpFile = new LpFile();
                lpFile.setIsDeleted(1);
                boolean fileResult = lpFileService.update(lpFile, 
                    new LambdaUpdateWrapper<LpFile>().eq(LpFile::getFileId, bookFileDTO.getFileId()));
                if (!fileResult) {
                    log.error("删除图书文件失败，fileId: " + bookFileDTO.getFileId());
                    return false;
                }
            } else {
                log.info("No book files found, skipping file deletion");
            }

            //检查是否存在图书章节，如存在则逻辑删除
            Long sectionCount = bookSectionService.count(
                new LambdaQueryWrapper<BookSection>()
                    .eq(BookSection::getBookId, id)
                    .eq(BookSection::getIsDeleted, 0)
            );
            if (sectionCount > 0) {
                //逻辑删除图书章节
                BookSection bookSection = new BookSection();
                bookSection.setIsDeleted(1);
                boolean sectionResult = bookSectionService.update(bookSection, 
                    new LambdaUpdateWrapper<BookSection>().eq(BookSection::getBookId, id));
                if (!sectionResult) {
                    return false;
                }
            } else {
                log.info("No book sections found, skipping section deletion");
            }

            //检查是否存在图书标签，如存在则逻辑删除
            Long topicCount = bookTopicService.count(
                new LambdaQueryWrapper<BookTopic>()
                    .eq(BookTopic::getBookId, id)
                    .eq(BookTopic::getIsDeleted, 0)
            );
            if (topicCount > 0) {
                //逻辑删除图书标签
                BookTopic bookTopic = new BookTopic();
                bookTopic.setIsDeleted(1);
                boolean topicResult = bookTopicService.update(bookTopic, 
                    new LambdaUpdateWrapper<BookTopic>().eq(BookTopic::getBookId, id));
                if (!topicResult) {
                    return false;
                }
            } else {
                log.info("No book topics found, skipping topic deletion");
            }
            
            //检查是否存在ES索引，如存在则删除
            try {
                //尝试删除ES索引，如果不存在会捕获异常
                bookSectionEsService.deleteByBookId(id.toString());
            } catch (Exception e) {
                //如果删除失败，记录日志但不影响整体流程
                log.info("No ES index found for bookId: " + id + ", skipping ES index deletion");
            }



            return true;
        } catch (Exception e) {
            log.error("删除图书失败，id: " + id, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除图书失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBook(BookUpdateRequest bookUpdateRequest) {
        try {
            // 1. 更新图书基本信息
            Book book = new Book();
            BeanUtils.copyProperties(bookUpdateRequest, book);
            boolean updateResult = bookMapper.updateById(book) > 0;
            if (!updateResult) {
                return false;
            }

            // 2. 处理文件关联关系
            if (bookUpdateRequest.getFileId() != null) {
                // 2.0 先检查是否已存在相同的文件关联记录
                BookFile existingBookFile = bookFileService.getOne(
                    new LambdaQueryWrapper<BookFile>()
                        .eq(BookFile::getBookId, book.getId())
                        .eq(BookFile::getFileId, bookUpdateRequest.getFileId())
                        .eq(BookFile::getIsDeleted, 0)
                );
                
                // 如果已存在相同的文件关联记录，则不需要进行任何操作
                if (existingBookFile == null) {
                    // 2.1 逻辑删除原有的关联记录
                    BookFile oldBookFile = new BookFile();
                    oldBookFile.setIsDeleted(1);
                    bookFileService.update(oldBookFile,
                        new LambdaUpdateWrapper<BookFile>()
                            .eq(BookFile::getBookId, book.getId())
                            .eq(BookFile::getIsDeleted, 0));
                    
                    // 2.2 新增新的关联记录
                    BookFile newBookFile = new BookFile();
                    newBookFile.setBookId(book.getId());
                    newBookFile.setFileId(bookUpdateRequest.getFileId());
                    boolean saveResult = bookFileService.save(newBookFile);
                    
                    if (!saveResult) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图书文件关联失败");
                    }
                }
            }

            return true;
        } catch (Exception e) {
            log.error("更新图书信息失败，bookId: " + bookUpdateRequest.getId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图书信息失败");
        }
    }

    @Override
    public Book getBookById(Long id) {
        return bookMapper.selectById(id);
    }

    @Override
    public List<Book> getBooksWithoutCover() {
        // 创建查询条件：picUrl为空且fileName不为空的图书
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(Book::getPicUrl)
                   .isNotNull(Book::getFileName)
                   .ne(Book::getFileName, "");
        
        return bookMapper.selectList(queryWrapper);
    }

    @Override
    public IPage<Book> listBooks(BookQueryRequest bookQueryRequest) {
        Page<Book> page = new Page<>(bookQueryRequest.getCurrent(), bookQueryRequest.getSize());
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(bookQueryRequest.getTitle())) {
            queryWrapper.like(Book::getTitle, bookQueryRequest.getTitle());
        }
        if (StringUtils.isNotBlank(bookQueryRequest.getIsbn())) {
            queryWrapper.like(Book::getIsbn, bookQueryRequest.getIsbn());
        }
        if (StringUtils.isNotBlank(bookQueryRequest.getAuthor())) {
            queryWrapper.like(Book::getAuthor, bookQueryRequest.getAuthor());
        }
        if (StringUtils.isNotBlank(bookQueryRequest.getCategory())) {
            queryWrapper.eq(Book::getCategory, bookQueryRequest.getCategory());
        }
        
        return bookMapper.selectPage(page, queryWrapper);
    }

    @Override
    public long countBooks(BookQueryRequest bookQueryRequest) {
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(bookQueryRequest.getTitle())) {
            queryWrapper.like(Book::getTitle, bookQueryRequest.getTitle());
        }
        if (StringUtils.isNotBlank(bookQueryRequest.getAuthor())) {
            queryWrapper.like(Book::getAuthor, bookQueryRequest.getAuthor());
        }
        if (StringUtils.isNotBlank(bookQueryRequest.getCategory())) {
            queryWrapper.eq(Book::getCategory, bookQueryRequest.getCategory());
        }
        
        return bookMapper.selectCount(queryWrapper);
    }

    @Override
    public IPage<Book> searchBooks(String keyword, int page, int size) {
        if (StringUtils.isBlank(keyword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        }

        Page<Book> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Book::getTitle, keyword)
                .or()
                .like(Book::getAuthor, keyword)
                .or()
                .like(Book::getSummary, keyword);
        
        return bookMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public IPage<Book> advancedSearch(String keyword, String category, String author, 
            Integer yearFrom, Integer yearTo, int page, int size) {
        Page<Book> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                .like(Book::getTitle, keyword)
                .or()
                .like(Book::getSummary, keyword));
        }
        if (StringUtils.isNotBlank(category)) {
            queryWrapper.eq(Book::getCategory, category);
        }
        if (StringUtils.isNotBlank(author)) {
            queryWrapper.like(Book::getAuthor, author);
        }
        if (yearFrom != null) {
            queryWrapper.ge(Book::getPublicationYear, yearFrom.toString());
        }
        if (yearTo != null) {
            queryWrapper.le(Book::getPublicationYear, yearTo.toString());
        }
        
        return bookMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public IPage<Book> templateSearch(String field, String value, int page, int size) {
        if (StringUtils.isAnyBlank(field, value)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索字段和值不能为空");
        }

        Page<Book> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        
        // 根据字段名动态构建查询条件
        switch (field) {
            case "title" -> queryWrapper.like(Book::getTitle, value);
            case "author" -> queryWrapper.like(Book::getAuthor, value);
            case "category" -> queryWrapper.eq(Book::getCategory, value);
            case "isbn" -> queryWrapper.eq(Book::getIsbn, value);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的搜索字段");
        }
        
        return bookMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public IPage<Book> nestedSearch(String keyword, Integer maxYear, int page, int size) {
        if (StringUtils.isBlank(keyword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        }

        Page<Book> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索
        queryWrapper.and(wrapper -> wrapper
            .like(Book::getTitle, keyword)
            .or()
            .like(Book::getAuthor, keyword)
            .or()
            .like(Book::getSummary, keyword));
        
        // 年份限制
        if (maxYear != null) {
            queryWrapper.le(Book::getPublicationYear, maxYear.toString());
        }
        
        return bookMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public String importBooksFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        try {
            // 读取文件内容
            byte[] fileContent = file.getBytes();
            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
            }

            // 调用新的方法处理
            return importBooksFromExcel(fileContent, fileName);
        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取文件失败：" + e.getMessage());
        }
    }

    @Override
    public String importBooksFromExcel(byte[] fileContent, String fileName) {
        if (fileContent == null || fileContent.length == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容不能为空");
        }

        Workbook workbook = null;

        try {
            // 根据文件名后缀创建对应的Workbook
            if (fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(new ByteArrayInputStream(fileContent));
            } else if (fileName.endsWith(".xls")) {
                workbook = new HSSFWorkbook(new ByteArrayInputStream(fileContent));
            } else {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的文件格式");
            }

            Sheet sheet = workbook.getSheetAt(0);
            List<Book> books = new ArrayList<>();
            int totalRows = 0;
            int successRows = 0;
            StringBuilder errorMsg = new StringBuilder();

            // 从第二行开始读取数据（第一行为表头）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                totalRows++;

                try {
                    Book book = new Book();
                    // 设置必填字段，处理标题中的冒号
                    String title = getCellValueAsString(row.getCell(0));
                    title = title.replaceAll("[：:]", " ");
                    if (StringUtils.isBlank(title)) {
                        errorMsg.append("第").append(i + 1).append("行：书名不能为空\n");
                        continue;
                    }
                    book.setTitle(title);

                    // 设置必填字段type,cellnum 16
                    String typeStr = getCellValueAsString(row.getCell(16));
                    //如果类型为空直接跳过
                    if (StringUtils.isBlank(typeStr)) {
                        errorMsg.append("第").append(i + 1).append("行：图书类型不能为空\n");
                        continue;
                    }
                    try {
                        Integer type = Integer.parseInt(typeStr);
                        book.setType(type);
                    } catch (NumberFormatException e) {
                        errorMsg.append("第").append(i + 1).append("行：图书类型必须为数字\n");
                        continue;
                    }

                    // 设置副标题
                    book.setSubTitle(getCellValueAsString(row.getCell(2)));

                    // 设置作者
                    book.setAuthor(getCellValueAsString(row.getCell(2)));

                    // 设置出版社
                    book.setPublisher(getCellValueAsString(row.getCell(3)));
                    
                    // 处理出版年份
                    String yearStr = getCellValueAsString(row.getCell(4));
                    if (StringUtils.isNotBlank(yearStr)) {
                        book.setPublicationYear(yearStr);
                    }

                    // 处理出版日期
                    String dateStr = getCellValueAsString(row.getCell(5));
                    if (StringUtils.isNotBlank(dateStr)) {
                        try {
                            // 验证日期格式
                            LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
                            book.setPublicationDate(dateStr);
                        } catch (DateTimeParseException e) {
                            errorMsg.append("第").append(i + 1).append("行：出版日期格式错误，应为yyyy-MM-dd格式\n");
                            continue;
                        }
                    }

                    // 设置ISBN
                    book.setIsbn(getCellValueAsString(row.getCell(6)));

                    // 设置类目
                    book.setCategory(getCellValueAsString(row.getCell(7)));

                    // 设置关键词
                    book.setKeyWord(getCellValueAsString(row.getCell(8)));

                    // 设置摘要
                    book.setSummary(getCellValueAsString(row.getCell(9)));

                    // 设置备注
                    book.setNote(getCellValueAsString(row.getCell(10)));

                    // 设置来源
                    book.setSource(getCellValueAsString(row.getCell(11)));

                    // 设置系列/丛编
                    book.setSeries(getCellValueAsString(row.getCell(12)));

                    book.setStatus(0); // 默认状态为正常

                    // 处理页数
                    String pageSizeStr = getCellValueAsString(row.getCell(13));
                    if (StringUtils.isNotBlank(pageSizeStr)) {
                        try {
                            book.setPageSize(Integer.parseInt(pageSizeStr));
                        } catch (NumberFormatException e) {
                            errorMsg.append("第").append(i + 1).append("行：页数格式错误\n");
                            continue;
                        }
                    }

                    // 处理评分
                    String scoreStr = getCellValueAsString(row.getCell(14));
                    if (StringUtils.isNotBlank(scoreStr)) {
                        try {
                            book.setScore(Integer.parseInt(scoreStr));
                        } catch (NumberFormatException e) {
                            errorMsg.append("第").append(i + 1).append("行：评分格式错误\n");
                            continue;
                        }
                    }

                    // 处理文件，如pdf文件路径不为空，则先将文件上传到minio，然后保存文件名
                    String filePath = getCellValueAsString(row.getCell(15));
                    if (StringUtils.isNotBlank(filePath)) {
                        log.info("需要处理filePath: " + filePath+",开始上传文件");
                        try {
                            String basePath = System.getProperty("user.dir") + "/filedata/";
                            // String basePath = System.getProperty("user.dir") + "/bookMS/src/main/resources/filedata/";
                            File file = new File(basePath + filePath);
                            if (!file.exists()) {
                                log.warn("文件不存在:{}，跳过文件上传。", file.getAbsolutePath());
                                errorMsg.append("第").append(i + 1).append("行：文件不存在，跳过上传\n");
                            } else {
                                log.info("resourcePath: " + basePath + filePath);
                                FileUploadResult fileUploadResult = minioUtils.uploadFile(file); // 使用MinioUtils替代FileService
                                log.info("fileUploadResult: " + fileUploadResult);
                                book.setFileName(fileUploadResult.getFilePath());
                            }
                        } catch (Exception e) {
                            log.error("文件上传失败: ", e);
                            errorMsg.append("第").append(i + 1).append("行：文件上传失败\n");
                        }
                    }

                    

                    books.add(book);
                    successRows++;
                } catch (Exception e) {
                    errorMsg.append("第").append(i + 1).append("行：处理失败 - ").append(e.getMessage()).append("\n");
                }
            }

            // 批量保存数据
            if (!books.isEmpty()) {
                for (Book book : books) {
                    bookMapper.insert(book);
                }
            }

            // 构建返回消息
            StringBuilder resultMsg = new StringBuilder();
            resultMsg.append("总处理行数: ").append(totalRows)
                    .append(", 成功导入: ").append(successRows)
                    .append(", 失败: ").append(totalRows - successRows);
            
            if (errorMsg.length() > 0) {
                resultMsg.append("\n错误信息:\n").append(errorMsg);
            }

            return resultMsg.toString();

        } catch (Exception e) {
            log.error("导入Excel失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "导入失败：" + e.getMessage());
        } finally {
            // 关闭资源
            try {
                if (workbook != null) {
                    workbook.close();
                }
            } catch (IOException e) {
                log.error("关闭资源失败", e);
            }
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        String cellValue = null;
        switch (cell.getCellType()) {
            case STRING -> cellValue = cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    cellValue = cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    cellValue = String.valueOf((long) cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> cellValue = String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cellValue = cell.getCellFormula();
        }
        return cellValue != null ? cellValue.trim() : null;
    }

    @Override
    public String getAndSaveOpacInfo(Long id) {
        // 获取图书信息
        Book book = getBookById(id);
        if (book == null || StringUtils.isBlank(book.getIsbn())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书不存在或ISBN为空");
        }

        // 判断type是否为1，如果不是则返回非书籍
        if (book.getType() != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该文件不是书籍，无法获取OPAC信息");
        }

        try {
            // 获取OPAC信息
            OpacBookInfo opacInfo = bookUtils.getBookInfoByISBN(book.getIsbn());
            if (opacInfo == null) {
                return "未找到相关OPAC信息，请检查网络连接";
            }

            // 更新图书信息
            BookUpdateRequest updateRequest = new BookUpdateRequest();
            updateRequest.setId(id);
            updateRequest.setTitle(opacInfo.getTitle().replaceAll("[：:]", " "));
            updateRequest.setPublisher(opacInfo.getPress());
            updateRequest.setPageSize(Integer.valueOf(opacInfo.getPageSize()));
            updateRequest.setPublicationYear(opacInfo.getYear());
            updateRequest.setSummary(opacInfo.getSummary());
            updateRequest.setCn(opacInfo.getCn());
            updateRequest.setAuthor(opacInfo.getAuthor());
            updateRequest.setTopic(opacInfo.getTopic());
            updateRequest.setOpacSeries(opacInfo.getSeries());
            updateRequest.setIsOpaced(1);

            // 保存更新
            boolean success = updateBook(updateRequest);
            if (!success) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图书信息失败");
            }

            return "OPAC信息获取成功";
        } catch (IOException e) {
            log.error("获取OPAC信息失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取OPAC信息失败：" + e.getMessage());
        }
    }

    @Override
    public String batchGetAndSaveOpacInfo() {
        // 查询未获取OPAC信息的图书
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(Book::getIsOpaced)
                   .or()
                   .ne(Book::getIsOpaced, 1);
        List<Book> books = bookMapper.selectList(queryWrapper);
        
        if (books.isEmpty()) {
            return "没有需要获取OPAC信息的图书";
        }

        int totalCount = books.size();
        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();

        // 遍历处理每本图书
        for (Book book : books) {
            try {
                if (StringUtils.isBlank(book.getIsbn())) {
                    errorMsg.append("图书ID: ").append(book.getId()).append(" - ISBN为空\n");
                    failCount++;
                    continue;
                }

                // 获取OPAC信息
                OpacBookInfo opacInfo = bookUtils.getBookInfoByISBN(book.getIsbn());
                if (opacInfo == null) {
                    errorMsg.append("图书ID: ").append(book.getId()).append("图书名称: ").append(book.getTitle()).append(" - 未找到OPAC信息\n");
                    //更新图书状态为未获取OPAC信息
                    book.setIsOpaced(2);
                    bookMapper.updateById(book);
                    failCount++;
                    continue;
                }

                // 更新图书信息
                BookUpdateRequest updateRequest = new BookUpdateRequest();
                updateRequest.setId(book.getId());
                updateRequest.setTitle(opacInfo.getTitle());
                updateRequest.setPublisher(opacInfo.getPress());
                updateRequest.setPageSize(Integer.valueOf(opacInfo.getPageSize()));
                updateRequest.setPublicationYear(opacInfo.getYear());
                updateRequest.setSummary(opacInfo.getSummary());
                updateRequest.setCn(opacInfo.getCn());
                updateRequest.setAuthor(opacInfo.getAuthor());
                updateRequest.setTopic(opacInfo.getTopic());
                updateRequest.setOpacSeries(opacInfo.getSeries());
                updateRequest.setIsOpaced(1);

                // 保存更新
                boolean success = updateBook(updateRequest);
                if (!success) {
                    errorMsg.append("图书ID: ").append(book.getId()).append("图书名称: ").append(book.getTitle()).append(" - 更新失败\n");
                    failCount++;
                    continue;
                }

                successCount++;
                // 添加延时，避免请求过于频繁
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("处理图书ID: " + book.getId() + " 失败", e);
                errorMsg.append("图书ID: ").append(book.getId()).append("图书名称: ").append(book.getTitle()).append(" - 处理失败: ").append(e.getMessage()).append("\n");
                failCount++;
            }
        }

        // 构建返回消息
        StringBuilder resultMsg = new StringBuilder();
        resultMsg.append("批量获取OPAC信息完成\n")
                .append("总数: ").append(totalCount).append("\n")
                .append("成功: ").append(successCount).append("\n")
                .append("失败: ").append(failCount);

        if (errorMsg.length() > 0) {
            resultMsg.append("<br>错误信息:<br>").append(errorMsg.toString().replace("\n", "<br>"));
        }

        return resultMsg.toString();
    }

    @Override
    public List<Book> getBooksWithoutOpac() {
        // 构建查询条件：is_opaced为空或为0
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
            .isNull(Book::getIsOpaced)
            .or()
            .eq(Book::getIsOpaced, 0)
        );
        queryWrapper.isNotNull(Book::getIsbn); // ISBN不能为空
        
        return this.list(queryWrapper);
    }

    @Override
    public List<Book> getBooksWithoutSections() {
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Book::getIsIndexed, 0)
                   .isNotNull(Book::getFileName);
        return list(queryWrapper);
    }


    @Override
    public IPage<BookVO> listBooksWithTags(BookQueryRequest bookQueryRequest) {
        // 1. 获取原始图书分页数据
        Page<Book> page = new Page<>(bookQueryRequest.getCurrent(), bookQueryRequest.getSize());
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        //获取is_deleted字段为0的记录
        queryWrapper.eq(Book::getIsDeleted, 0);   
        
        // 添加查询条件
        if (StringUtils.isNotBlank(bookQueryRequest.getTitle())) {
            queryWrapper.like(Book::getTitle, bookQueryRequest.getTitle());
        }
        if (StringUtils.isNotBlank(bookQueryRequest.getIsbn())) {
            queryWrapper.like(Book::getIsbn, bookQueryRequest.getIsbn());
        }
        if (StringUtils.isNotBlank(bookQueryRequest.getAuthor())) {
            queryWrapper.like(Book::getAuthor, bookQueryRequest.getAuthor());
        }
        if (StringUtils.isNotBlank(bookQueryRequest.getPublisher())) {
            queryWrapper.like(Book::getPublisher, bookQueryRequest.getPublisher());
        }
        if (StringUtils.isNotBlank(bookQueryRequest.getSource())) {
            queryWrapper.eq(Book::getSource, bookQueryRequest.getSource());
        }
        //出版年份
        if (StringUtils.isNotBlank(bookQueryRequest.getPublicationYear())) {
            queryWrapper.eq(Book::getPublicationYear, bookQueryRequest.getPublicationYear());
        }
        
        // 处理多选类目
        List<String> categoryList = bookQueryRequest.getCategoryList();
        if (categoryList != null && !categoryList.isEmpty()) {
            queryWrapper.in(Book::getCategory, categoryList);
        } else if (StringUtils.isNotBlank(bookQueryRequest.getCategory())) {
            queryWrapper.eq(Book::getCategory, bookQueryRequest.getCategory());
        }

        // 添加type字段的查询条件
        if (bookQueryRequest.getType() != null) {
            queryWrapper.eq(Book::getType, bookQueryRequest.getType());
        }

        // 添加按照最后修改时间倒序排列
        queryWrapper.orderByDesc(Book::getModifiedTime);
        
        IPage<Book> bookPage = bookMapper.selectPage(page, queryWrapper);
        
        // 2. 转换为BookVO并添加标签信息
        List<BookVO> bookVOList = bookPage.getRecords().stream().map(book -> {
            BookVO bookVO = new BookVO();
            BeanUtils.copyProperties(book, bookVO);
            // 获取图书标签
            bookVO.setTags(topicService.getBookTopicTags(book.getId()));
            return bookVO;
        }).collect(Collectors.toList());
        
        // 3. 构建BookVO的分页对象
        Page<BookVO> bookVOPage = new Page<>(bookPage.getCurrent(), bookPage.getSize(), bookPage.getTotal());
        bookVOPage.setRecords(bookVOList);
        
        return bookVOPage;
    }

    @Override
    public List<String> getAllNonEmptyCategories() {
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Book::getCategory)
                .isNotNull(Book::getCategory)
                .ne(Book::getCategory, "")
                .groupBy(Book::getCategory);
        
        return this.list(queryWrapper)
                .stream()
                .map(Book::getCategory)
                .filter(category -> category != null && !category.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public IPage<BookVO> convertToBookVOPage(IPage<Book> bookPage) {
        // 1. 首先，将LpBook转换为BookVO
        List<BookVO> bookVOList = bookPage.getRecords().stream().map(book -> {
            BookVO bookVO = new BookVO();
            BeanUtils.copyProperties(book, bookVO);
            // 获取图书标签
            bookVO.setTags(topicService.getBookTopicTags(book.getId()));
            return bookVO;
        }).collect(Collectors.toList());
        
        // 2. 构建BookVO的分页对象
        Page<BookVO> bookVOPage = new Page<>(bookPage.getCurrent(), bookPage.getSize(), bookPage.getTotal());
        bookVOPage.setRecords(bookVOList);
        
        return bookVOPage;
    }
    
    @Override
    public List<Book> listBooksForTopicParsing() {
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNotNull(Book::getTopic)
                .ne(Book::getTopic, "")
                .and(w -> w.isNull(Book::getHasParseOPACTopic)
                        .or()
                        .eq(Book::getHasParseOPACTopic, 0))
                .eq(Book::getIsDeleted, 0); // 只查询未删除的图书
        
        return bookMapper.selectList(queryWrapper);
    }
    
    @Override
    public boolean parseBookOPACTopics(Long bookId) {
        // 1. 获取图书信息
        Book book = getById(bookId);
        if (book == null || book.getTopic() == null || book.getTopic().isEmpty()) {
            log.warn("图书不存在或无主题字段，bookId={}", bookId);
            return false;
        }
        
        try {
            // 2. 调用主题服务解析OPAC主题
            boolean result = topicService.parseBookOPACTopics(bookId, book.getTopic());
            
            // 3. 更新图书的主题解析状态
            if (result) {
                return markTopicParseStatus(bookId, true);
            } else {
                log.warn("解析图书主题失败，bookId={}", bookId);
                return markTopicParseStatus(bookId, false);
            }
        } catch (Exception e) {
            log.error("解析图书主题出错，bookId={}", bookId, e);
            markTopicParseStatus(bookId, false);
            return false;
        }
    }
    
    @Override
    public boolean markTopicParseStatus(Long bookId, boolean success) {
        // 更新图书的主题解析状态
        Book updateBook = new Book();
        updateBook.setId(bookId);
        updateBook.setHasParseOPACTopic(success ? 1 : 2); // 1表示成功，2表示失败
        
        return updateById(updateBook);
    }
} 