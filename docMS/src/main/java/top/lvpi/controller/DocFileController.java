package top.lvpi.controller;

import top.lvpi.common.BaseResponse;
import top.lvpi.common.ResultUtils;
import top.lvpi.model.dto.file.DocFileDTO;
import top.lvpi.service.DocFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doc-file")
@Tag(name = "书籍文件关联管理")
public class DocFileController {

    @Autowired
    private DocFileService docFileService;

    @PostMapping("/save")
    @Operation(summary = "保存书籍文件关联")
    public BaseResponse<Long> saveDocFile(@Validated @RequestBody DocFileDTO docFileDTO) {
        Long id = docFileService.saveDocFile(docFileDTO);
        return ResultUtils.success(id);
    }

    @PostMapping("/saveBatch")
    @Operation(summary = "批量保存书籍文件关联")
    public BaseResponse<Boolean> saveBatchDocFile(@Validated @RequestBody List<DocFileDTO> docFileDTOList) {
        boolean result = docFileService.saveBatchDocFile(docFileDTOList);
        return ResultUtils.success(result);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除书籍文件关联")
    public BaseResponse<Boolean> deleteDocFile(@PathVariable Long id) {
        boolean result = docFileService.deleteDocFile(id);
        return ResultUtils.success(result);
    }

    @GetMapping("/doc/{docId}")
    @Operation(summary = "获取书籍的文件关联列表")
    public BaseResponse<List<DocFileDTO>> getDocFilesByDocId(@PathVariable Long docId) {
        List<DocFileDTO> docFiles = docFileService.getDocFilesByDocId(docId);
        return ResultUtils.success(docFiles);
    }

    @GetMapping("/file/{fileId}")
    @Operation(summary = "获取文件的书籍关联列表")
    public BaseResponse<List<DocFileDTO>> getDocFilesByFileId(@PathVariable Long fileId) {
        List<DocFileDTO> docFiles = docFileService.getDocFilesByFileId(fileId);
        return ResultUtils.success(docFiles);
    }
} 