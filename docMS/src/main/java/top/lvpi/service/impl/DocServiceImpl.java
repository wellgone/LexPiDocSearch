package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.lvpi.common.BusinessException;
import top.lvpi.common.ErrorCode;
import top.lvpi.mapper.DocMapper;
import top.lvpi.mapper.DocSectionMapper;
import top.lvpi.model.dto.doc.DocAddRequest;
import top.lvpi.model.dto.doc.DocQueryRequest;
import top.lvpi.model.dto.doc.DocUpdateRequest;
import top.lvpi.model.dto.file.DocFileDTO;
import top.lvpi.model.dto.file.FileUploadResult;
import top.lvpi.model.entity.Doc;
import top.lvpi.model.entity.DocFile;
import top.lvpi.model.entity.DocSection;
import top.lvpi.model.entity.DocTopic;
import top.lvpi.model.entity.LpFile;
import top.lvpi.model.entity.OpacDocInfo;
import top.lvpi.model.vo.DocVO;
import top.lvpi.service.DocFileService;
import top.lvpi.service.DocSectionEsService;
import top.lvpi.service.DocSectionService;
import top.lvpi.service.DocService;
import top.lvpi.service.DocTopicService;
import top.lvpi.service.ImgService;
import top.lvpi.service.LpFileService;
import top.lvpi.service.TopicService;
import top.lvpi.utils.DocUtils;
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
public class DocServiceImpl extends ServiceImpl<DocMapper, Doc> implements DocService {

    @Autowired
    private MinioUtils minioUtils;

    @Autowired
    private DocMapper docMapper;

    @Autowired
    private DocSectionMapper docSectionMapper;

    @Autowired
    private DocUtils docUtils;

    @Autowired
    private ImgService imgService;

    @Autowired
    private LpFileService lpFileService;

    @Autowired
    private DocFileService docFileService;

    @Autowired
    private DocSectionEsService docSectionEsService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private DocTopicService docTopicService;

    @Autowired
    private DocSectionService docSectionService;

    @Value("${file.upload-dir:/app/filedata}")
    private String uploadDir;

    @Override
    public boolean hasDocSections(Long docId) {
        LambdaQueryWrapper<DocSection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocSection::getDocId, docId);
        return docSectionMapper.selectCount(queryWrapper) > 0;
    }


    @Override
    public int addDoc(DocAddRequest docAddRequest) {
        LpDoc doc = new LpDoc();
        BeanUtils.copyProperties(docAddRequest, doc);
        try {
            int result = docMapper.insert(doc);
            if (result <= 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增图书失败");
            }
            //判断是否存在文件id
            if (docAddRequest.getFileId() != null) {
                //关联图书与文件
                LpDocFile lpDocFile = new LpDocFile();
                lpDocFile.setDocId(doc.getId());
                lpDocFile.setFileId(docAddRequest.getFileId());
                boolean save = lpDocFileService.save(lpDocFile);
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
    public boolean deleteDoc(Long id) {
        try {
            //逻辑删除图书
            Doc doc = new Doc();
            doc.setId(id);
            doc.setIsDeleted(1);
            boolean docResult = this.updateById(doc);
            if (!docResult) {
                return false;
            }
            //检查是否存在关联图片
            Doc docData = this.getDocById(id);
            if (docData.getPicUrl() != null) {
                Long imgId = Long.parseLong(docData.getPicUrl().replace("img/", ""));
                if (imgId != null) {
                    boolean imgResult = imgService.removeById(imgId);
                    if (!imgResult) {
                        log.error("删除图书封面失败，imgId: " + imgId);
                        return false;
                    }
                }
            }

            //检查是否存在图书文件关联表，如存在则逻辑删除
            Long fileAssocCount = docFileService.count(
                new LambdaQueryWrapper<DocFile>()
                    .eq(DocFile::getDocId, id)
                    .eq(DocFile::getIsDeleted, 0)
            );
            if (fileAssocCount > 0) {   
                //逻辑删除图书文件关联表
                DocFile docFile = new DocFile();
                docFile.setIsDeleted(1);
                boolean fileAssocResult = docFileService.update(docFile, 
                new LambdaUpdateWrapper<DocFile>().eq(DocFile::getDocId, id));
                if (!fileAssocResult) {
                    log.error("删除图书文件关联表失败，docId: " + id);
                    return false;
                }
            } else {
                log.info("No doc files found, skipping file deletion");
            }

            //检查是否存在文件表，如存在则逻辑删除
            DocFileDTO docFileDTO = docFileService.getDocFilesNoDeleteByDocId(id);
            if (docFileDTO != null) {
                LpFile lpFile = new LpFile();
                lpFile.setIsDeleted(1);
                boolean fileResult = lpFileService.update(lpFile, 
                    new LambdaUpdateWrapper<LpFile>().eq(LpFile::getFileId, docFileDTO.getFileId()));
                if (!fileResult) {
                    log.error("删除图书文件失败，fileId: " + docFileDTO.getFileId());
                    return false;
                }
            } else {
                log.info("No doc files found, skipping file deletion");
            }

            //检查是否存在图书章节，如存在则逻辑删除
            Long sectionCount = docSectionService.count(
                new LambdaQueryWrapper<DocSection>()
                    .eq(DocSection::getDocId, id)
                    .eq(DocSection::getIsDeleted, 0)
            );
            if (sectionCount > 0) {
                //逻辑删除图书章节
                DocSection docSection = new DocSection();
                docSection.setIsDeleted(1);
                boolean sectionResult = docSectionService.update(docSection, 
                    new LambdaUpdateWrapper<DocSection>().eq(DocSection::getDocId, id));
                if (!sectionResult) {
                    return false;
                }
            } else {
                log.info("No doc sections found, skipping section deletion");
            }

            //检查是否存在图书标签，如存在则逻辑删除
            Long topicCount = docTopicService.count(
                new LambdaQueryWrapper<DocTopic>()
                    .eq(DocTopic::getDocId, id)
                    .eq(DocTopic::getIsDeleted, 0)
            );
            if (topicCount > 0) {
                //逻辑删除图书标签
                DocTopic docTopic = new DocTopic();
                docTopic.setIsDeleted(1);
                boolean topicResult = docTopicService.update(docTopic, 
                    new LambdaUpdateWrapper<DocTopic>().eq(DocTopic::getDocId, id));
                if (!topicResult) {
                    return false;
                }
            } else {
                log.info("No doc topics found, skipping topic deletion");
            }
            
            //检查是否存在ES索引，如存在则删除
            try {
                //尝试删除ES索引，如果不存在会捕获异常
                docSectionEsService.deleteByDocId(id.toString());
            } catch (Exception e) {
                //如果删除失败，记录日志但不影响整体流程
                log.info("No ES index found for docId: " + id + ", skipping ES index deletion");
            }



            return true;
        } catch (Exception e) {
            log.error("删除图书失败，id: " + id, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除图书失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDoc(DocUpdateRequest docUpdateRequest) {
        try {
            // 1. 更新图书基本信息
            Doc doc = new Doc();
            BeanUtils.copyProperties(docUpdateRequest, doc);
            boolean updateResult = docMapper.updateById(doc) > 0;
            if (!updateResult) {
                return false;
            }

            // 2. 处理文件关联关系
            if (docUpdateRequest.getFileId() != null) {
                // 2.0 先检查是否已存在相同的文件关联记录
                DocFile existingDocFile = docFileService.getOne(
                    new LambdaQueryWrapper<DocFile>()
                        .eq(DocFile::getDocId, doc.getId())
                        .eq(DocFile::getFileId, docUpdateRequest.getFileId())
                        .eq(DocFile::getIsDeleted, 0)
                );
                
                // 如果已存在相同的文件关联记录，则不需要进行任何操作
                if (existingDocFile == null) {
                    // 2.1 逻辑删除原有的关联记录
                    DocFile oldDocFile = new DocFile();
                    oldDocFile.setIsDeleted(1);
                    docFileService.update(oldDocFile,
                        new LambdaUpdateWrapper<DocFile>()
                            .eq(DocFile::getDocId, doc.getId())
                            .eq(DocFile::getIsDeleted, 0));
                    
                    // 2.2 新增新的关联记录
                    DocFile newDocFile = new DocFile();
                    newDocFile.setDocId(doc.getId());
                    newDocFile.setFileId(docUpdateRequest.getFileId());
                    boolean saveResult = docFileService.save(newDocFile);
                    
                    if (!saveResult) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图书文件关联失败");
                    }
                }
            }

            return true;
        } catch (Exception e) {
            log.error("更新图书信息失败，docId: " + docUpdateRequest.getId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图书信息失败");
        }
    }

    @Override
    public Doc getDocById(Long id) {
        return docMapper.selectById(id);
    }

    @Override
    public List<Doc> getDocsWithoutCover() {
        // 创建查询条件：picUrl为空且fileName不为空的图书
        LambdaQueryWrapper<Doc> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(Doc::getPicUrl)
                   .isNotNull(Doc::getFileName)
                   .ne(Doc::getFileName, "");
        
        return docMapper.selectList(queryWrapper);
    }

    @Override
    public IPage<Doc> listDocs(DocQueryRequest docQueryRequest) {
        Page<Doc> page = new Page<>(docQueryRequest.getCurrent(), docQueryRequest.getSize());
        LambdaQueryWrapper<Doc> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(docQueryRequest.getTitle())) {
            queryWrapper.like(Doc::getTitle, docQueryRequest.getTitle());
        }
        if (StringUtils.isNotBlank(docQueryRequest.getIsbn())) {
            queryWrapper.like(Doc::getIsbn, docQueryRequest.getIsbn());
        }
        if (StringUtils.isNotBlank(docQueryRequest.getAuthor())) {
            queryWrapper.like(Doc::getAuthor, docQueryRequest.getAuthor());
        }
        if (StringUtils.isNotBlank(docQueryRequest.getCategory())) {
            queryWrapper.eq(Doc::getCategory, docQueryRequest.getCategory());
        }
        
        return docMapper.selectPage(page, queryWrapper);
    }

    @Override
    public long countDocs(DocQueryRequest docQueryRequest) {
        LambdaQueryWrapper<Doc> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(docQueryRequest.getTitle())) {
            queryWrapper.like(Doc::getTitle, docQueryRequest.getTitle());
        }
        if (StringUtils.isNotBlank(docQueryRequest.getAuthor())) {
            queryWrapper.like(Doc::getAuthor, docQueryRequest.getAuthor());
        }
        if (StringUtils.isNotBlank(docQueryRequest.getCategory())) {
            queryWrapper.eq(Doc::getCategory, docQueryRequest.getCategory());
        }
        
        return docMapper.selectCount(queryWrapper);
    }

    @Override
    public IPage<Doc> searchDocs(String keyword, int page, int size) {
        if (StringUtils.isBlank(keyword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        }

        Page<Doc> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Doc> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Doc::getTitle, keyword)
                .or()
                .like(Doc::getAuthor, keyword)
                .or()
                .like(Doc::getSummary, keyword);
        
        return docMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public IPage<Doc> advancedSearch(String keyword, String category, String author, 
            Integer yearFrom, Integer yearTo, int page, int size) {
        Page<Doc> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Doc> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                .like(Doc::getTitle, keyword)
                .or()
                .like(Doc::getSummary, keyword));
        }
        if (StringUtils.isNotBlank(category)) {
            queryWrapper.eq(Doc::getCategory, category);
        }
        if (StringUtils.isNotBlank(author)) {
            queryWrapper.like(Doc::getAuthor, author);
        }
        if (yearFrom != null) {
            queryWrapper.ge(Doc::getPublicationYear, yearFrom.toString());
        }
        if (yearTo != null) {
            queryWrapper.le(Doc::getPublicationYear, yearTo.toString());
        }
        
        return docMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public IPage<Doc> templateSearch(String field, String value, int page, int size) {
        if (StringUtils.isAnyBlank(field, value)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索字段和值不能为空");
        }

        Page<Doc> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Doc> queryWrapper = new LambdaQueryWrapper<>();
        
        // 根据字段名动态构建查询条件
        switch (field) {
            case "title" -> queryWrapper.like(Doc::getTitle, value);
            case "author" -> queryWrapper.like(Doc::getAuthor, value);
            case "category" -> queryWrapper.eq(Doc::getCategory, value);
            case "isbn" -> queryWrapper.eq(Doc::getIsbn, value);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的搜索字段");
        }
        
        return docMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public IPage<Doc> nestedSearch(String keyword, Integer maxYear, int page, int size) {
        if (StringUtils.isBlank(keyword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        }

        Page<Doc> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Doc> queryWrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索
        queryWrapper.and(wrapper -> wrapper
            .like(Doc::getTitle, keyword)
            .or()
            .like(Doc::getAuthor, keyword)
            .or()
            .like(Doc::getSummary, keyword));
        
        // 年份限制
        if (maxYear != null) {
            queryWrapper.le(Doc::getPublicationYear, maxYear.toString());
        }
        
        return docMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public String importDocsFromExcel(MultipartFile file) {
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
            return importDocsFromExcel(fileContent, fileName);
        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取文件失败：" + e.getMessage());
        }
    }

    @Override
    public String importDocsFromExcel(byte[] fileContent, String fileName) {
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
            List<Doc> docs = new ArrayList<>();
            int totalRows = 0;
            int successRows = 0;
            StringBuilder errorMsg = new StringBuilder();

            // 从第二行开始读取数据（第一行为表头）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                totalRows++;

                try {
                    Doc doc = new Doc();
                    // 设置必填字段，处理标题中的冒号
                    String title = getCellValueAsString(row.getCell(0));
                    title = title.replaceAll("[：:]", " ");
                    if (StringUtils.isBlank(title)) {
                        errorMsg.append("第").append(i + 1).append("行：书名不能为空\n");
                        continue;
                    }
                    doc.setTitle(title);

                    // 设置必填字段type,cellnum 16
                    String typeStr = getCellValueAsString(row.getCell(16));
                    //如果类型为空直接跳过
                    if (StringUtils.isBlank(typeStr)) {
                        errorMsg.append("第").append(i + 1).append("行：图书类型不能为空\n");
                        continue;
                    }
                    try {
                        Integer type = Integer.parseInt(typeStr);
                        doc.setType(type);
                    } catch (NumberFormatException e) {
                        errorMsg.append("第").append(i + 1).append("行：图书类型必须为数字\n");
                        continue;
                    }

                    // 设置副标题
                    doc.setSubTitle(getCellValueAsString(row.getCell(2)));

                    // 设置作者
                    doc.setAuthor(getCellValueAsString(row.getCell(2)));

                    // 设置出版社
                    doc.setPublisher(getCellValueAsString(row.getCell(3)));
                    
                    // 处理出版年份
                    String yearStr = getCellValueAsString(row.getCell(4));
                    if (StringUtils.isNotBlank(yearStr)) {
                        doc.setPublicationYear(yearStr);
                    }

                    // 处理出版日期
                    String dateStr = getCellValueAsString(row.getCell(5));
                    if (StringUtils.isNotBlank(dateStr)) {
                        try {
                            // 验证日期格式
                            LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
                            doc.setPublicationDate(dateStr);
                        } catch (DateTimeParseException e) {
                            errorMsg.append("第").append(i + 1).append("行：出版日期格式错误，应为yyyy-MM-dd格式\n");
                            continue;
                        }
                    }

                    // 设置ISBN
                    doc.setIsbn(getCellValueAsString(row.getCell(6)));

                    // 设置类目
                    doc.setCategory(getCellValueAsString(row.getCell(7)));

                    // 设置关键词
                    doc.setKeyWord(getCellValueAsString(row.getCell(8)));

                    // 设置摘要
                    doc.setSummary(getCellValueAsString(row.getCell(9)));

                    // 设置备注
                    doc.setNote(getCellValueAsString(row.getCell(10)));

                    // 设置来源
                    doc.setSource(getCellValueAsString(row.getCell(11)));

                    // 设置系列/丛编
                    doc.setSeries(getCellValueAsString(row.getCell(12)));

                    doc.setStatus(0); // 默认状态为正常

                    // 处理页数
                    String pageSizeStr = getCellValueAsString(row.getCell(13));
                    if (StringUtils.isNotBlank(pageSizeStr)) {
                        try {
                            doc.setPageSize(Integer.parseInt(pageSizeStr));
                        } catch (NumberFormatException e) {
                            errorMsg.append("第").append(i + 1).append("行：页数格式错误\n");
                            continue;
                        }
                    }

                    // 处理评分
                    String scoreStr = getCellValueAsString(row.getCell(14));
                    if (StringUtils.isNotBlank(scoreStr)) {
                        try {
                            doc.setScore(Integer.parseInt(scoreStr));
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
                            // String basePath = System.getProperty("user.dir") + "/docMS/src/main/resources/filedata/";
                            File file = new File(basePath + filePath);
                            if (!file.exists()) {
                                log.warn("文件不存在:{}，跳过文件上传。", file.getAbsolutePath());
                                errorMsg.append("第").append(i + 1).append("行：文件不存在，跳过上传\n");
                            } else {
                                log.info("resourcePath: " + basePath + filePath);
                                FileUploadResult fileUploadResult = minioUtils.uploadFile(file); // 使用MinioUtils替代FileService
                                log.info("fileUploadResult: " + fileUploadResult);
                                doc.setFileName(fileUploadResult.getFileName());
                            }
                        } catch (Exception e) {
                            log.error("文件上传失败: ", e);
                            errorMsg.append("第").append(i + 1).append("行：文件上传失败\n");
                        }
                    }

                    

                    docs.add(doc);
                    successRows++;
                } catch (Exception e) {
                    errorMsg.append("第").append(i + 1).append("行：处理失败 - ").append(e.getMessage()).append("\n");
                }
            }

            // 批量保存数据
            if (!docs.isEmpty()) {
                for (Doc doc : docs) {
                    docMapper.insert(doc);
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
        Doc doc = getDocById(id);
        if (doc == null || StringUtils.isBlank(doc.getIsbn())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书不存在或ISBN为空");
        }

        // 判断type是否为1，如果不是则返回非书籍
        if (doc.getType() != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该文件不是书籍，无法获取OPAC信息");
        }

        try {
            // 获取OPAC信息
            OpacDocInfo opacInfo = docUtils.getDocInfoByISBN(doc.getIsbn());
            if (opacInfo == null) {
                return "未找到相关OPAC信息，请检查网络连接";
            }

            // 更新图书信息
            DocUpdateRequest updateRequest = new DocUpdateRequest();
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
            boolean success = updateDoc(updateRequest);
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
        LambdaQueryWrapper<Doc> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(Doc::getIsOpaced)
                   .or()
                   .ne(Doc::getIsOpaced, 1);
        List<Doc> docs = docMapper.selectList(queryWrapper);
        
        if (docs.isEmpty()) {
            return "没有需要获取OPAC信息的图书";
        }

        int totalCount = docs.size();
        int successCount = 0;
        int failCount = 0;
        StringBuilder errorMsg = new StringBuilder();

        // 遍历处理每本图书
        for (Doc doc : docs) {
            try {
                if (StringUtils.isBlank(doc.getIsbn())) {
                    errorMsg.append("图书ID: ").append(doc.getId()).append(" - ISBN为空\n");
                    failCount++;
                    continue;
                }

                // 获取OPAC信息
                OpacDocInfo opacInfo = docUtils.getDocInfoByISBN(doc.getIsbn());
                if (opacInfo == null) {
                    errorMsg.append("图书ID: ").append(doc.getId()).append("图书名称: ").append(doc.getTitle()).append(" - 未找到OPAC信息\n");
                    //更新图书状态为未获取OPAC信息
                    doc.setIsOpaced(2);
                    docMapper.updateById(doc);
                    failCount++;
                    continue;
                }

                // 更新图书信息
                DocUpdateRequest updateRequest = new DocUpdateRequest();
                updateRequest.setId(doc.getId());
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
                boolean success = updateDoc(updateRequest);
                if (!success) {
                    errorMsg.append("图书ID: ").append(doc.getId()).append("图书名称: ").append(doc.getTitle()).append(" - 更新失败\n");
                    failCount++;
                    continue;
                }

                successCount++;
                // 添加延时，避免请求过于频繁
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("处理图书ID: " + doc.getId() + " 失败", e);
                errorMsg.append("图书ID: ").append(doc.getId()).append("图书名称: ").append(doc.getTitle()).append(" - 处理失败: ").append(e.getMessage()).append("\n");
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
    public List<Doc> getDocsWithoutOpac() {
        // 构建查询条件：is_opaced为空或为0
        LambdaQueryWrapper<Doc> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
            .isNull(Doc::getIsOpaced)
            .or()
            .eq(Doc::getIsOpaced, 0)
        );
        queryWrapper.isNotNull(Doc::getIsbn); // ISBN不能为空
        
        return this.list(queryWrapper);
    }

    @Override
    public List<Doc> getDocsWithoutSections() {
        LambdaQueryWrapper<Doc> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Doc::getIsIndexed, 0)
                   .isNotNull(Doc::getFileName);
        return list(queryWrapper);
    }


    @Override
    public IPage<DocVO> listDocsWithTags(DocQueryRequest docQueryRequest) {
        // 1. 获取原始图书分页数据
        Page<Doc> page = new Page<>(docQueryRequest.getCurrent(), docQueryRequest.getSize());
        LambdaQueryWrapper<Doc> queryWrapper = new LambdaQueryWrapper<>();
        //获取is_deleted字段为0的记录
        queryWrapper.eq(Doc::getIsDeleted, 0);   
        
        // 添加查询条件
        if (StringUtils.isNotBlank(docQueryRequest.getTitle())) {
            queryWrapper.like(Doc::getTitle, docQueryRequest.getTitle());
        }
        if (StringUtils.isNotBlank(docQueryRequest.getIsbn())) {
            queryWrapper.like(Doc::getIsbn, docQueryRequest.getIsbn());
        }
        if (StringUtils.isNotBlank(docQueryRequest.getAuthor())) {
            queryWrapper.like(Doc::getAuthor, docQueryRequest.getAuthor());
        }
        if (StringUtils.isNotBlank(docQueryRequest.getPublisher())) {
            queryWrapper.like(Doc::getPublisher, docQueryRequest.getPublisher());
        }
        if (StringUtils.isNotBlank(docQueryRequest.getSource())) {
            queryWrapper.eq(Doc::getSource, docQueryRequest.getSource());
        }
        //出版年份
        if (StringUtils.isNotBlank(docQueryRequest.getPublicationYear())) {
            queryWrapper.eq(Doc::getPublicationYear, docQueryRequest.getPublicationYear());
        }
        
        // 处理多选类目
        List<String> categoryList = docQueryRequest.getCategoryList();
        if (categoryList != null && !categoryList.isEmpty()) {
            queryWrapper.in(Doc::getCategory, categoryList);
        } else if (StringUtils.isNotBlank(docQueryRequest.getCategory())) {
            queryWrapper.eq(Doc::getCategory, docQueryRequest.getCategory());
        }

        // 添加type字段的查询条件
        if (docQueryRequest.getType() != null) {
            queryWrapper.eq(Doc::getType, docQueryRequest.getType());
        }

        // 添加按照最后修改时间倒序排列
        queryWrapper.orderByDesc(Doc::getModifiedTime);
        
        IPage<Doc> docPage = docMapper.selectPage(page, queryWrapper);
        
        // 2. 转换为DocVO并添加标签信息
        List<DocVO> docVOList = docPage.getRecords().stream().map(doc -> {
            DocVO docVO = new DocVO();
            BeanUtils.copyProperties(doc, docVO);
            // 获取图书标签
            docVO.setTags(topicService.getDocTopicTags(doc.getId()));
            return docVO;
        }).collect(Collectors.toList());
        
        // 3. 构建DocVO的分页对象
        Page<DocVO> docVOPage = new Page<>(docPage.getCurrent(), docPage.getSize(), docPage.getTotal());
        docVOPage.setRecords(docVOList);
        
        return docVOPage;
    }

    @Override
    public List<String> getAllNonEmptyCategories() {
        LambdaQueryWrapper<Doc> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Doc::getCategory)
                .isNotNull(Doc::getCategory)
                .ne(Doc::getCategory, "")
                .groupBy(Doc::getCategory);
        
        return this.list(queryWrapper)
                .stream()
                .map(Doc::getCategory)
                .filter(category -> category != null && !category.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public IPage<DocVO> convertToDocVOPage(IPage<Doc> docPage) {
        // 1. 首先，将LpDoc转换为DocVO
        List<DocVO> docVOList = docPage.getRecords().stream().map(doc -> {
            DocVO docVO = new DocVO();
            BeanUtils.copyProperties(doc, docVO);
            // 获取图书标签
            docVO.setTags(topicService.getDocTopicTags(doc.getId()));
            return docVO;
        }).collect(Collectors.toList());
        
        // 2. 构建DocVO的分页对象
        Page<DocVO> docVOPage = new Page<>(docPage.getCurrent(), docPage.getSize(), docPage.getTotal());
        docVOPage.setRecords(docVOList);
        
        return docVOPage;
    }
    
    @Override
    public List<Doc> listDocsForTopicParsing() {
        LambdaQueryWrapper<Doc> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNotNull(Doc::getTopic)
                .ne(Doc::getTopic, "")
                .and(w -> w.isNull(Doc::getHasParseOPACTopic)
                        .or()
                        .eq(Doc::getHasParseOPACTopic, 0))
                .eq(Doc::getIsDeleted, 0); // 只查询未删除的图书
        
        return docMapper.selectList(queryWrapper);
    }
    
    @Override
    public boolean parseDocOPACTopics(Long docId) {
        // 1. 获取图书信息
        Doc doc = getById(docId);
        if (doc == null || doc.getTopic() == null || doc.getTopic().isEmpty()) {
            log.warn("图书不存在或无主题字段，docId={}", docId);
            return false;
        }
        
        try {
            // 2. 调用主题服务解析OPAC主题
            boolean result = topicService.parseDocOPACTopics(docId, doc.getTopic());
            
            // 3. 更新图书的主题解析状态
            if (result) {
                return markTopicParseStatus(docId, true);
            } else {
                log.warn("解析图书主题失败，docId={}", docId);
                return markTopicParseStatus(docId, false);
            }
        } catch (Exception e) {
            log.error("解析图书主题出错，docId={}", docId, e);
            markTopicParseStatus(docId, false);
            return false;
        }
    }
    
    @Override
    public boolean markTopicParseStatus(Long docId, boolean success) {
        // 更新图书的主题解析状态
        Doc updateDoc = new Doc();
        updateDoc.setId(docId);
        updateDoc.setHasParseOPACTopic(success ? 1 : 2); // 1表示成功，2表示失败
        
        return updateById(updateDoc);
    }
} 