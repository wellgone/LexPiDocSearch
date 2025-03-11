package top.lvpi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.lvpi.common.BaseResponse;
import top.lvpi.common.BusinessException;
import top.lvpi.common.ErrorCode;
import top.lvpi.model.dto.doc.DocAddRequest;
import top.lvpi.model.dto.doc.DocQueryRequest;
import top.lvpi.model.dto.doc.DocUpdateRequest;
import top.lvpi.model.entity.Doc;
import top.lvpi.model.entity.LpFile;
import top.lvpi.model.vo.DocVO;
import top.lvpi.service.DocService;
import top.lvpi.service.FileService;
import top.lvpi.service.PdfService;
import top.lvpi.utils.PDFUtils;

import cn.hutool.core.lang.UUID;

import top.lvpi.service.ImgService;
import top.lvpi.service.LpFileService;
import top.lvpi.model.dto.file.DocFileDTO;
import top.lvpi.model.dto.file.FileUploadResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.scheduling.annotation.EnableAsync;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import java.net.URLEncoder;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Arrays;

import top.lvpi.model.dto.task.TaskProgress;
import top.lvpi.service.DocFileService;
import top.lvpi.service.DocSectionEsService;

@Tag(name = "图书管理", description = "图书相关接口")
@RestController
@RequestMapping("/doc")
@Slf4j
@EnableAsync
public class DocController {

    @Autowired
    private DocService docService;

    @Autowired
    private FileService fileService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private ImgService imgService;

    @Autowired
    private LpFileService lpFileService;

    @Autowired
    private DocSectionEsService docSectionEsService;

    @Autowired
    private DocFileService docFileService;

    // 修改为存储TaskProgress的映射
    private final ConcurrentHashMap<String, TaskProgress> taskProgressMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<String>> taskResults = new ConcurrentHashMap<>();

    @Operation(summary = "添加图书", description = "添加新图书")
    @PostMapping("/add")
    public BaseResponse<Integer> addDoc(@RequestBody DocAddRequest docAddRequest) {
        if (docAddRequest == null) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String title = docAddRequest.getTitle();
        if (StringUtils.isBlank(title)) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "书名不能为空");
        }
        try {
            int result = docService.addDoc(docAddRequest);
            return BaseResponse.success(result);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "删除图书", description = "根据ID删除图书")
    @DeleteMapping("/delete/{id}")
    public BaseResponse<Boolean> deleteDoc(@PathVariable("id") Long id) {
        if (id <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "ID不合法");
        }
        try {
            boolean result = docService.deleteDoc(id);
            return BaseResponse.success(result);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "更新图书", description = "更新图书信息")
    @PutMapping("/update")
    public BaseResponse<Boolean> updateDoc(@RequestBody DocUpdateRequest docUpdateRequest) {
        if (docUpdateRequest == null || docUpdateRequest.getId() == null) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        try {
            boolean result = docService.updateDoc(docUpdateRequest);
            return BaseResponse.success(result);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "获取图书", description = "根据ID获取图书详情")
    @GetMapping("/get/{id}")
    public BaseResponse<Doc> getDocById(@PathVariable("id") Long id) {
        if (id <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "ID不合法");
        }
        try {
            Doc doc = docService.getDocById(id);
            if (doc == null) {
                return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "图书不存在");
            }
            return BaseResponse.success(doc);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "图书列表", description = "获取图书列表")
    @GetMapping("/list")
    public BaseResponse<IPage<DocVO>> listDocs(DocQueryRequest docQueryRequest) {
        if (docQueryRequest == null) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        try {

            // 处理多选类目参数
            String categoryStr = docQueryRequest.getCategory();
            if (StringUtils.isNotBlank(categoryStr)) {
                String[] categories = categoryStr.split(",");
                docQueryRequest.setCategoryList(Arrays.asList(categories));
            }
            
            IPage<DocVO> docPage = docService.listDocsWithTags(docQueryRequest);
            return BaseResponse.success(docPage);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "获取所有非空类目", description = "获取数据库中所有非空的图书类目")
    @GetMapping("/categories")
    public BaseResponse<List<String>> getCategories() {
        try {
            List<String> categories = docService.getAllNonEmptyCategories();
            return BaseResponse.success(categories);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "搜索图书", description = "关键词搜索")
    @GetMapping("/search")
    public BaseResponse<IPage<Doc>> searchDocs(
            @Parameter(description = "搜索关键词") @RequestParam("keyword") String keyword,
            @Parameter(description = "页码") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(value = "size", defaultValue = "10") int size) {
        
        if (StringUtils.isBlank(keyword)) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        }
        if (page <= 0 || size <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }
        try {
            IPage<Doc> docPage = docService.searchDocs(keyword, page, size);
            return BaseResponse.success(docPage);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "高级搜索", description = "多条件组合搜索图书")
    @GetMapping("/search/advanced")
    public BaseResponse<IPage<Doc>> advancedSearch(
            @Parameter(description = "关键词") @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "分类") @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "作者") @RequestParam(value = "author", required = false) String author,
            @Parameter(description = "起始年份") @RequestParam(value = "yearFrom", required = false) Integer yearFrom,
            @Parameter(description = "结束年份") @RequestParam(value = "yearTo", required = false) Integer yearTo,
            @Parameter(description = "页码") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(value = "size", defaultValue = "10") int size) {
        
        if (page <= 0 || size <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }
        // 验证年份范围
        if (yearFrom != null && yearTo != null && yearFrom > yearTo) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "年份范围错误");
        }
        try {
            IPage<Doc> docPage = docService.advancedSearch(keyword, category, author, yearFrom, yearTo, page, size);
            return BaseResponse.success(docPage);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "模板搜索", description = "根据指定字段搜索图书")
    @GetMapping("/search/template")
    public BaseResponse<IPage<Doc>> templateSearch(
            @Parameter(description = "字段名") @RequestParam("field") String field,
            @Parameter(description = "字段值") @RequestParam("value") String value,
            @Parameter(description = "页码") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(value = "size", defaultValue = "10") int size) {
        
        if (StringUtils.isAnyBlank(field, value)) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "搜索字段和值不能为空");
        }
        if (page <= 0 || size <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }
        try {
            IPage<Doc> docPage = docService.templateSearch(field, value, page, size);
            return BaseResponse.success(docPage);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "嵌套搜索", description = "关键词和年份组合搜索")
    @GetMapping("/search/nested")
    public BaseResponse<IPage<Doc>> nestedSearch(
            @Parameter(description = "搜索关键词") @RequestParam("keyword") String keyword,
            @Parameter(description = "最大年份") @RequestParam(value = "maxYear", required = false) Integer maxYear,
            @Parameter(description = "页码") @RequestParam(value = "page", defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(value = "size", defaultValue = "10") int size) {
        
        if (StringUtils.isBlank(keyword)) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        }
        if (page <= 0 || size <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "分页参数错误");
        }
        try {
            IPage<Doc> docPage = docService.nestedSearch(keyword, maxYear, page, size);
            return BaseResponse.success(docPage);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "统计图书数量", description = "根据查询条件统计图书数量")
    @GetMapping("/count")
    public BaseResponse<Long> countDocs(DocQueryRequest docQueryRequest) {
        if (docQueryRequest == null) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        try {
            long count = docService.countDocs(docQueryRequest);
            return BaseResponse.success(count);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "上传图书文件", description = "上传PDF文件并返回文件路径")
    @PostMapping("/upload/{id}")
    public BaseResponse<String> uploadDoc(
            @Parameter(description = "图书ID") @PathVariable("id") Long id,
            @Parameter(description = "PDF文件") @RequestParam("file") MultipartFile file) {
        if (id == null || id <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "图书ID不合法");
        }
        if (file == null || file.isEmpty()) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        // 检查文件类型
        String contentType = file.getContentType();
        if (!"application/pdf".equals(contentType)) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "只支持PDF文件");
        }
        try {
            // 上传文件
            FileUploadResult result = fileService.uploadFile(file);
            
            // 更新图书信息
            DocUpdateRequest updateRequest = new DocUpdateRequest();
            updateRequest.setId(id);
            updateRequest.setFileName(result.getFileName());
            updateRequest.setPageSize(result.getPageCount());
            docService.updateDoc(updateRequest);
            
            return BaseResponse.success(result.getFileName());
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    
    @Operation(summary = "上传文件", description = "上传文件并返回文件id")
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(
            @Parameter(description = "PDF文件") @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        // 检查文件类型
        String contentType = file.getContentType();
        if (!"application/pdf".equals(contentType)) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "只支持PDF文件");
        }
        //构建文件信息对象
        LpFile lpFile = new LpFile();
        lpFile.setFileOriginalName(file.getOriginalFilename());
        lpFile.setFileSize(file.getSize());
        lpFile.setFileSuffix(file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")));
        try {
            lpFile.setFileMd5(DigestUtils.md5Hex(file.getBytes()));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件md5计算失败");
        }

        try {
            // 上传文件
            FileUploadResult result = fileService.uploadFile(file);
            
            // 更新文件信息
            lpFile.setFileName(result.getFileName());

            // 保存文件信息
            try {
                boolean save = lpFileService.save(lpFile);
                if (!save) {
                    return BaseResponse.error(ErrorCode.OPERATION_ERROR, "文件信息保存失败");
                }
                //返回文件id
                return BaseResponse.success(lpFile.getFileId().toString());
            } catch (BusinessException e) {
                return BaseResponse.error(e.getCode(), e.getMessage());
            }
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "上传图书封面", description = "上传图书封面图片并返回图片id")
    @PostMapping("/upload/cover")
    public BaseResponse<String> uploadCover(
            @Parameter(description = "base64图片数据") @RequestBody Map<String, String> requestBody) {
        String base64Data = requestBody.get("img");
        if (StringUtils.isBlank(base64Data)) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "图片数据不能为空");
        }
        try {
            // 保存图片数据到数据库
            Long imgId = imgService.saveImage(base64Data, null);
            
            // 更新图书信息
            // DocUpdateRequest updateRequest = new DocUpdateRequest();
            // updateRequest.setId(id);
            // updateRequest.setPicUrl("img/" + imgId); // 设置图片访问路径
            // docService.updateDoc(updateRequest);
            
            return BaseResponse.success("img/" + imgId);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "获取图书封面", description = "获取图书封面base64数据")
    @GetMapping("/cover/{id}")
    public BaseResponse<String> getDocCover(
            @Parameter(description = "图书ID") @PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "图书ID不合法");
        }
        try {
            String base64Data = imgService.getImageByDocId(id);
            if (base64Data == null) {
                return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "图书封面不存在");
            }
            return BaseResponse.success(base64Data);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "获取图书封面-图片ID", description = "通过图片ID获取图书封面base64数据")
    @GetMapping("/cover/img/{imgId}")
    public BaseResponse<String> getDocCoverByImgId(
            @Parameter(description = "图片ID") @PathVariable("imgId") Long imgId) {
        if (imgId == null || imgId <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "图片ID不合法");
        }
        try {
            String base64Data = imgService.getImageById(imgId);
            if (base64Data == null) {
                return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "图书封面不存在");
            }
            return BaseResponse.success(base64Data);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }
    
    @Operation(summary = "下载图书", description = "根据文件路径下载图书")
    @GetMapping("/download/{id}")
    public void downloadDoc(
            @Parameter(description = "图书ID") @PathVariable("id") Long id,
            HttpServletResponse response) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图书ID不合法");
        }
        try {
            // 获取图书id
            DocFileDTO docFileDTO = docFileService.getDocFilesNoDeleteByDocId(id);
            if (docFileDTO == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书文件不存在");
            }
            //根据图书id获取file信息
            LpFile lpFile = lpFileService.getById(docFileDTO.getFileId());
            if (lpFile == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在");
            }
            // 获取文件名
            String fileName = lpFile.getFileName();
            
            // 设置响应头
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", 
                "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));
            
            // 获取文件流并写入响应
            fileService.downloadFile(fileName, response.getOutputStream());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("下载文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载文件失败");
        }
    }
    

    @SuppressWarnings("unchecked")
    @Operation(summary = "提取文本", description = "提取PDF文件中的文本内容")
    @PostMapping("/extract/{id}")
    public BaseResponse<String> extractText(
            @Parameter(description = "图书ID") @PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "图书ID不合法");
        }

        // 先检查是否已存在章节
        if (docService.hasDocSections(id)) {
            return BaseResponse.error(ErrorCode.OPERATION_ERROR, "该PDF文件已进行过文本提取");
        }

        // 检查是否有正在进行中的任务
        for (TaskProgress progress : taskProgressMap.values()) {
            if (progress.getStatus() == 0) {
                try {
                    Doc doc = docService.getDocById(id);
                    if (doc != null && StringUtils.isNotBlank(doc.getFileName())) {
                        return BaseResponse.error(ErrorCode.OPERATION_ERROR, "该PDF文件正在进行文本提取，请稍后再试");
                    }
                } catch (BusinessException e) {
                    log.error("检查任务状态时发生错误", e);
                }
            }
        }

        // 生成唯一的任务ID
        String taskId = UUID.randomUUID().toString();
        
        // 初始化任务进度
        TaskProgress taskProgress = new TaskProgress()
            .setTaskId(taskId)
            .setStatus(0)
            .setProgress(0)
            .setCurrentStep("初始化任务");
        taskProgressMap.put(taskId, taskProgress);
        
        // 提交异步任务
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {


                DocFileDTO docFileDTO = docFileService.getDocFilesNoDeleteByDocId(id);   
                if (docFileDTO == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书文件不存在");
                }

                LpFile lpFile = lpFileService.getById(docFileDTO.getFileId());
                if (lpFile == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在");
                }
                

                // 更新进度：检查封面
                taskProgress.setProgress(10)
                    .setCurrentStep("检查图书封面");
                
                Doc doc = docService.getDocById(id);
                // 检查是否需要提取封面
                if (StringUtils.isBlank(doc.getPicUrl())) {
                    taskProgress.setCurrentStep("提取PDF封面");
                    try {
                        // 获取PDF文件输入流
                        InputStream pdfStream = fileService.getFileInputStream(lpFile.getFileName());
                        // 提取PDF首页为图片
                        String base64Image = PDFUtils.exportPageToImage(pdfStream);
                        // 保存图片到数据库
                        Long imgId = imgService.saveImage(base64Image, id);
                        // 更新图书封面URL
                        DocUpdateRequest updateRequest = new DocUpdateRequest();
                        updateRequest.setId(id);
                        updateRequest.setPicUrl("img/" + imgId);
                        docService.updateDoc(updateRequest);
                        pdfStream.close();
                    } catch (Exception e) {
                        log.error("提取PDF封面失败", e);
                    }
                }

                // 更新进度：开始文本提取
                taskProgress.setProgress(30)
                    .setCurrentStep("正在提取PDF文本内容...");

                // 执行文本提取
                String result = pdfService.extractText(lpFile.getFileName(), id, doc.getTitle());

                // 更新任务完成状态
                taskProgress.setStatus(1)
                    .setProgress(100)
                    .setCurrentStep("文本提取完成")
                    //返回图书id、名称、文件名
                    .setResult(id + "," + doc.getTitle() + "," + lpFile.getFileName());

                return result;
            } catch (BusinessException e) {
                // 更新任务失败状态
                taskProgress.setStatus(2)
                    .setErrorMessage(e.getMessage());
                throw new RuntimeException(e);
            }
        });

        // 将任务结果存储到映射中
        taskResults.put(taskId, future);

        // 返回任务ID
        return BaseResponse.success(20001,"任务已开始，任务ID:" + taskId);
    }

    @Operation(summary = "查询提取结果", description = "根据任务ID查询提取结果")
    @GetMapping("/extract/result/{taskId}")
    public BaseResponse<TaskProgress> getExtractResult(@PathVariable("taskId") String taskId) {
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress == null) {
            return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "任务ID不存在");
        }

        // 如果任务已完成，从映射中移除
        if (progress.getStatus() != 0) {
            taskProgressMap.remove(taskId);
            taskResults.remove(taskId);
        }

        return BaseResponse.success(progress);
    }

    @Operation(summary = "获取文件预览地址--图书id", description = "通过图书id获取预览地址")
    @GetMapping("/preview/{id}")
    public BaseResponse<String> getPreviewUrl(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "图书ID不合法");
        }
        
        try {
            // 获取文件信息
            DocFileDTO docFileDTO = docFileService.getDocFilesNoDeleteByDocId(id);
            if (docFileDTO == null) {
                return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "图书文件不存在");
            }

            LpFile lpFile = lpFileService.getById(docFileDTO.getFileId());
            if (lpFile == null) {
                return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "文件不存在");
            }
            
            // 获取预览地址
            String previewUrl = fileService.getPreviewUrl(lpFile.getFileName());
            return BaseResponse.success(previewUrl); // 返回预览地址
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "获取文件预览地址--文件名", description = "通过文件名获取预览地址")
    @GetMapping("/preview/fileName/{fileName}")
    public BaseResponse<String> getPreviewUrlByFileName(
            @Parameter(description = "文件名") @PathVariable("fileName") String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }
        
        try {
            // 获取预览地址
            String previewUrl = fileService.getPreviewUrl(fileName);
            return BaseResponse.success(previewUrl);
        } catch (BusinessException e) {
            return BaseResponse.error(e.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "批量导入图书", description = "通过Excel文件批量导入图书信息")
    @PostMapping("/import/excel")
    public BaseResponse<String> importDocsFromExcel(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        
        // 检查文件类型
        String fileName = file.getOriginalFilename();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "只支持Excel文件(.xlsx或.xls)");
        }

        try {
            byte[] fileContent = file.getBytes();
            String taskId = UUID.randomUUID().toString();
            
            // 初始化任务进度
            TaskProgress taskProgress = new TaskProgress()
                .setTaskId(taskId)
                .setStatus(0)
                .setProgress(0)
                .setCurrentStep("开始导入Excel文件");
            taskProgressMap.put(taskId, taskProgress);
            
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // 更新进度：导入基本信息
                    taskProgress.setProgress(20)
                        .setCurrentStep("导入图书基本信息");
                    String importResult = docService.importDocsFromExcel(fileContent, fileName);
                    
                    // 更新进度：处理封面
                    taskProgress.setProgress(50)
                        .setCurrentStep("获取需要处理封面的图书列表");
                    List<Doc> docsWithoutCover = docService.getDocsWithoutCover();
                    
                    int total = docsWithoutCover.size();
                    int current = 0;
                    
                    // 处理每本书的封面
                    for (Doc doc : docsWithoutCover) {
                        current++;
                        int progress = 50 + (current * 50 / total);
                        taskProgress.setProgress(progress)
                            .setCurrentStep(String.format("正在处理第%d/%d本图书的封面", current, total));
                        
                        //根据图书id获取file信息
                        DocFileDTO docFileDTO = docFileService.getDocFilesNoDeleteByDocId(doc.getId());
                        if (docFileDTO == null) {
                            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书文件不存在");
                        }

                        LpFile lpFile = lpFileService.getById(docFileDTO.getFileId());
                        if (lpFile == null) {
                            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在");
                        }

                        //获取文件流
                        InputStream pdfStream = fileService.getFileInputStream(lpFile.getFileName());
                        
                        try {
                            String base64Image = PDFUtils.exportPageToImage(pdfStream);
                            Long imgId = imgService.saveImage(base64Image, doc.getId());
                            DocUpdateRequest updateRequest = new DocUpdateRequest();
                            updateRequest.setId(doc.getId());
                            updateRequest.setPicUrl("img/" + imgId);
                            docService.updateDoc(updateRequest);
                            pdfStream.close();
                        } catch (Exception e) {
                            log.error("提取PDF封面失败，图书ID:" + doc.getId(), e);
                        }
                        
                    }
                    
                    // 更新任务完成状态
                    String result = importResult + "\n封面提取完成，成功处理 " + total + " 本图书";
                    taskProgress.setStatus(1)
                        .setProgress(100)
                        .setCurrentStep("导入完成")
                        .setResult(result);
                    
                    return result;
                } catch (BusinessException e) {
                    // 更新任务失败状态
                    taskProgress.setStatus(2)
                        .setErrorMessage(e.getMessage());
                    throw new RuntimeException(e);
                } catch (IOException e1) {
                    log.error("读取文件失败", e1);
                    throw new RuntimeException(e1);
                }
            });
            
            taskResults.put(taskId, future);
            return BaseResponse.success(20001, "任务已开始，任务ID:" + taskId);
        } catch (IOException e) {
            log.error("读取文件失败", e);
            return BaseResponse.error(ErrorCode.SYSTEM_ERROR, "读取文件失败：" + e.getMessage());
        }
    }

    @Operation(summary = "查询导入任务状态", description = "根据任务ID查询导入任务的执行状态")
    @GetMapping("/import/status/{taskId}")
    public BaseResponse<TaskProgress> getImportStatus(@PathVariable("taskId") String taskId) {
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress == null) {
            return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "任务ID不存在");
        }

        // 如果任务已完成，从映射中移除
        if (progress.getStatus() != 0) {
            taskProgressMap.remove(taskId);
            taskResults.remove(taskId);
        }

        return BaseResponse.success(progress);
    }

    @Operation(summary = "获取图书OPAC信息", description = "根据ISBN获取图书OPAC信息")
    @PostMapping("/opac/{id}")
    public BaseResponse<String> getDocOpacInfo(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return BaseResponse.error(ErrorCode.PARAMS_ERROR, "图书ID不合法");
        }
        
        // 获取图书信息
        Doc doc = docService.getDocById(id);
        if (doc == null || StringUtils.isBlank(doc.getIsbn())) {
            return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "图书不存在或ISBN为空");
        }
        
        // 检查是否已获取过OPAC信息
        if (doc.getIsOpaced() != null && doc.getIsOpaced() == 1) {
            return BaseResponse.error(ErrorCode.OPERATION_ERROR, "该图书已获取过OPAC信息");
        }

        // 判断type是否为1，如果不是则返回非书籍
        if (doc.getType() != 1) {
            return BaseResponse.error(ErrorCode.OPERATION_ERROR, "该文件不是书籍，无法获取OPAC信息");
        }

        // 生成唯一的任务ID
        String taskId = UUID.randomUUID().toString();
        
        // 初始化任务进度
        TaskProgress taskProgress = new TaskProgress()
            .setTaskId(taskId)
            .setStatus(0)
            .setProgress(0)
            .setCurrentStep("初始化OPAC信息获取任务");
        taskProgressMap.put(taskId, taskProgress);
        
        // 提交异步任务
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                // 更新进度：开始获取OPAC信息
                taskProgress.setProgress(30)
                    .setCurrentStep("正在获取OPAC信息");
                
                String result = docService.getAndSaveOpacInfo(id);
                
                // 更新任务完成状态
                taskProgress.setStatus(1)
                    .setProgress(100)
                    .setCurrentStep("OPAC信息获取完成")
                    .setResult(result);
                
                return result;
            } catch (BusinessException e) {
                // 更新任务失败状态
                taskProgress.setStatus(2)
                    .setErrorMessage(e.getMessage());
                throw new RuntimeException(e);
            }
        });
        
        // 将任务结果存储到映射中
        taskResults.put(taskId, future);
        
        // 返回任务ID
        return BaseResponse.success(20001, "任务已开始，任务ID:" + taskId);
    }

    @Operation(summary = "查询OPAC获取任务状态", description = "根据任务ID查询OPAC获取任务的执行状态")
    @GetMapping("/opac/status/{taskId}")
    public BaseResponse<TaskProgress> getOpacStatus(@PathVariable("taskId") String taskId) {
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress == null) {
            return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "任务ID不存在");
        }
        
        // 如果任务已完成，从映射中移除
        if (progress.getStatus() != 0) {
            taskProgressMap.remove(taskId);
            taskResults.remove(taskId);
        }
        
        return BaseResponse.success(progress);
    }

    @Operation(summary = "批量获取OPAC信息", description = "获取所有未获取OPAC信息的图书信息")
    @PostMapping("/opac/batch")
    public BaseResponse<String> batchGetOpacInfo() {
        // 生成唯一的任务ID
        String taskId = UUID.randomUUID().toString();
        
        // 初始化任务进度
        TaskProgress taskProgress = new TaskProgress()
            .setTaskId(taskId)
            .setStatus(0)
            .setProgress(0)
            .setCurrentStep("初始化批量OPAC信息获取任务");
        taskProgressMap.put(taskId, taskProgress);
        
        // 提交异步任务
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                // 更新进度：开始获取待处理图书列表
                taskProgress.setProgress(10)
                    .setCurrentStep("获取待处理图书列表");
                
                List<Doc> docs = docService.getDocsWithoutOpac();
                int total = docs.size();
                
                if (total == 0) {
                    taskProgress.setStatus(1)
                        .setProgress(100)
                        .setCurrentStep("没有需要获取OPAC信息的图书")
                        .setResult("完成：没有需要获取OPAC信息的图书");
                    return "没有需要获取OPAC信息的图书";
                }
                
                // 更新进度：开始批量处理
                taskProgress.setProgress(20)
                    .setCurrentStep(String.format("开始处理%d本图书的OPAC信息", total));
                
                int current = 0;
                StringBuilder resultBuilder = new StringBuilder();
                
                // 处理每本书的OPAC信息
                for (Doc doc : docs) {
                    current++;
                    int progress = 20 + (current * 80 / total);
                    taskProgress.setProgress(progress)
                        .setCurrentStep(String.format("正在处理第%d/%d本图书的OPAC信息", current, total));
                    
                    try {
                        String result = docService.getAndSaveOpacInfo(doc.getId());
                        resultBuilder.append(String.format("图书ID:%d，名称：%s - %s\n", doc.getId(), doc.getTitle(), result));
                    } catch (Exception e) {
                        resultBuilder.append(String.format("图书ID:%d，名称：%s - 处理失败: %s\n", doc.getId(), doc.getTitle(), e.getMessage()));
                    }
                }
                
                // 更新任务完成状态
                String finalResult = String.format("批量处理完成，共处理%d本图书。\n详细结果：\n%s", total, resultBuilder.toString());
                taskProgress.setStatus(1)
                    .setProgress(100)
                    .setCurrentStep("批量OPAC信息获取完成")
                    .setResult(finalResult);
                
                return finalResult;
            } catch (BusinessException e) {
                // 更新任务失败状态
                taskProgress.setStatus(2)
                    .setErrorMessage(e.getMessage());
                throw new RuntimeException(e);
            }
        });
        
        // 将任务结果存储到映射中
        taskResults.put(taskId, future);
        
        // 返回任务ID
        return BaseResponse.success(20001, "任务已开始，任务ID:" + taskId);
    }

    @Operation(summary = "查询批量OPAC获取任务状态", description = "根据任务ID查询批量OPAC获取任务的执行状态")
    @GetMapping("/opac/batch/status/{taskId}")
    public BaseResponse<TaskProgress> getBatchOpacStatus(@PathVariable("taskId") String taskId) {
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress == null) {
            return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "任务ID不存在");
        }
        
        // 如果任务已完成，从映射中移除
        if (progress.getStatus() != 0) {
            taskProgressMap.remove(taskId);
            taskResults.remove(taskId);
        }
        
        return BaseResponse.success(progress);
    }

    @Operation(summary = "批量提取和导入", description = "批量提取PDF文本并导入到ES")
    @PostMapping("/batch/extract-import")
    public BaseResponse<String> handleBatchExtractAndImport() {
        // 生成唯一的任务ID
        String taskId = UUID.randomUUID().toString();
        
        // 初始化任务进度
        TaskProgress taskProgress = new TaskProgress()
            .setTaskId(taskId)
            .setStatus(0)
            .setProgress(0)
            .setCurrentStep("初始化批量提取任务");
        taskProgressMap.put(taskId, taskProgress);
        
        // 提交异步任务
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                // 获取未提取文本的图书列表
                List<Doc> docs = docService.getDocsWithoutSections();
                int total = docs.size();
                
                if (total == 0) {
                    taskProgress.setStatus(1)
                        .setProgress(100)
                        .setCurrentStep("没有需要提取的图书")
                        .setResult("完成：没有需要提取的图书");
                    return "没有需要提取的图书";
                }
                
                int current = 0;
                StringBuilder resultBuilder = new StringBuilder();
                
                // 处理每本书
                for (Doc doc : docs) {
                    current++;
                    int progress = (current * 100) / (total * 2); // 总进度分为提取和导入两部分
                    taskProgress.setProgress(progress)
                        .setCurrentStep(String.format("正在提取第%d/%d本图书", current, total));

                    //根据图书id获取file信息
                    DocFileDTO docFileDTO = docFileService.getDocFilesNoDeleteByDocId(doc.getId());
                    if (docFileDTO == null) {
                        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书文件不存在");
                    }

                    LpFile lpFile = lpFileService.getById(docFileDTO.getFileId());
                    if (lpFile == null) {
                        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在");
                    }

                    try {
                        if (lpFile != null) {
                            String result = pdfService.extractText(lpFile.getFileName(), doc.getId(), doc.getTitle());
                            resultBuilder.append(String.format("图书ID:%d，名称：%s - 提取成功\n", doc.getId(), doc.getTitle()));
                        }
                    } catch (Exception e) {
                        resultBuilder.append(String.format("图书ID:%d，名称：%s - 提取失败: %s\n", doc.getId(), doc.getTitle(), e.getMessage()));
                    }
                }

                // 开始导入到ES
                taskProgress.setCurrentStep("开始导入到ES");
                current = 0;
                
                for (Doc doc : docs) {
                    current++;
                    int progress = 50 + (current * 100) / (total * 2); // 导入部分占后50%进度
                    taskProgress.setProgress(progress)
                        .setCurrentStep(String.format("正在导入第%d/%d本图书到ES", current, total));
                    
                    try {
                        if (doc != null) {
                            // 导入到ES
                            docSectionEsService.importDocSections(doc.getId());
                            resultBuilder.append(String.format("图书ID:%d，名称：%s - 导入ES成功\n", doc.getId(), doc.getTitle()));
                            
                            // 更新图书状态为已索引
                            DocUpdateRequest updateRequest = new DocUpdateRequest();
                            updateRequest.setId(doc.getId());
                            updateRequest.setIsIndexed(1);
                            docService.updateDoc(updateRequest);
                        }
                    } catch (Exception e) {
                        resultBuilder.append(String.format("图书ID:%d，名称：%s - 导入ES失败: %s\n", doc.getId(), doc.getTitle(), e.getMessage()));
                    }
                }
                
                // 更新任务完成状态
                String finalResult = String.format("批量处理完成，共处理%d本图书。\n详细结果：\n%s", total, resultBuilder.toString());
                taskProgress.setStatus(1)
                    .setProgress(100)
                    .setCurrentStep("批量提取和导入完成")
                    .setResult(finalResult);
                
                return finalResult;
            } catch (BusinessException e) {
                taskProgress.setStatus(2)
                    .setErrorMessage(e.getMessage());
                throw new RuntimeException(e);
            }
        });
        
        taskResults.put(taskId, future);
        return BaseResponse.success(20001, "任务已开始，任务ID:" + taskId);
    }

    @Operation(summary = "查询批量提取和导入任务状态", description = "根据任务ID查询批量提取和导入任务的执行状态")
    @GetMapping("/batch/extract-import/status/{taskId}")
    public BaseResponse<TaskProgress> getBatchExtractImportStatus(@PathVariable("taskId") String taskId) {
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress == null) {
            return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "任务ID不存在");
        }
        
        // 如果任务已完成，从映射中移除
        if (progress.getStatus() != 0) {
            taskProgressMap.remove(taskId);
            taskResults.remove(taskId);
        }
        
        return BaseResponse.success(progress);
    }

    @Operation(summary = "批量自动解析OPAC主题标签", description = "自动解析所有未处理的OPAC主题标签信息")
    @PostMapping("/opac/topics/batch")
    public BaseResponse<String> batchParseOPACTopics() {
        // 生成唯一的任务ID
        String taskId = UUID.randomUUID().toString();
        
        // 初始化任务进度
        TaskProgress taskProgress = new TaskProgress()
            .setTaskId(taskId)
            .setStatus(0)
            .setProgress(0)
            .setCurrentStep("初始化批量解析OPAC主题标签任务");
        taskProgressMap.put(taskId, taskProgress);
        
        // 提交异步任务
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                // 更新进度：开始获取待处理图书列表
                taskProgress.setProgress(10)
                    .setCurrentStep("获取待处理图书列表");
                
                // 查询需要处理的图书（has_parse_opac_topic为null或0，且topic字段不为null的记录）
                List<Doc> docs = docService.listDocsForTopicParsing();
                
                if (docs.isEmpty()) {
                    taskProgress.setProgress(100)
                        .setStatus(2)
                        .setCurrentStep("没有需要解析OPAC主题标签的图书");
                    return "没有需要解析OPAC主题标签的图书";
                }
                
                int total = docs.size();
                log.info("找到{}本需要解析OPAC主题标签的图书", total);
                
                // 更新进度：开始处理
                taskProgress.setProgress(20)
                    .setCurrentStep("开始解析OPAC主题标签，共" + total + "本图书");
                
                int processed = 0;
                int success = 0;
                int failed = 0;
                
                // 逐个处理图书
                for (Doc doc : docs) {
                    try {
                        if (doc.getTopic() != null && !doc.getTopic().isEmpty()) {
                            // 解析主题标签
                            boolean result = docService.parseDocOPACTopics(doc.getId());
                            if (result) {
                                success++;
                            } else {
                                failed++;
                            }
                        } else {
                            // 没有主题，标记为已处理
                            docService.markTopicParseStatus(doc.getId(), false);
                            failed++;
                        }
                    } catch (Exception e) {
                        log.error("解析图书ID={}的OPAC主题标签时出错", doc.getId(), e);
                        failed++;
                    }
                    
                    // 更新处理进度
                    processed++;
                    int progress = 20 + (processed * 80 / total);
                    taskProgress.setProgress(progress)
                        .setCurrentStep(String.format("已处理 %d/%d 本图书，成功：%d，失败：%d", 
                                        processed, total, success, failed));
                }
                
                // 任务完成
                taskProgress.setProgress(100)
                    .setStatus(1)
                    .setCurrentStep(String.format("任务完成，共处理 %d 本图书，成功：%d，失败：%d", 
                                total, success, failed));
                
                return String.format("批量解析OPAC主题标签完成，共处理 %d 本图书，成功：%d，失败：%d", 
                                     total, success, failed);
                
            } catch (Exception e) {
                log.error("批量解析OPAC主题标签时出错", e);
                taskProgress.setStatus(-1)
                    .setCurrentStep("处理失败：" + e.getMessage());
                return "处理失败：" + e.getMessage();
            }
        });
        
        // 存储任务结果
        taskResults.put(taskId, future);
        
        return BaseResponse.success(20001, "任务已开始，任务ID:" + taskId);
    }
    
    @Operation(summary = "查询批量解析OPAC主题标签任务状态", description = "根据任务ID查询批量解析OPAC主题标签任务的执行状态")
    @GetMapping("/opac/topics/batch/status/{taskId}")
    public BaseResponse<TaskProgress> getBatchParseOPACTopicsStatus(@PathVariable("taskId") String taskId) {
        TaskProgress progress = taskProgressMap.get(taskId);
        if (progress == null) {
            return BaseResponse.error(ErrorCode.NOT_FOUND_ERROR, "任务ID不存在");
        }
        
        // 如果任务已完成，从映射中移除
        if (progress.getStatus() != 0) {
            taskProgressMap.remove(taskId);
            taskResults.remove(taskId);
        }
        
        return BaseResponse.success(progress);
    }



} 