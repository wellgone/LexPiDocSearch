package top.lvpi.controller;

import top.lvpi.common.BaseResponse;
import top.lvpi.common.ResultUtils;
import top.lvpi.model.dto.file.BookFileDTO;
import top.lvpi.service.BookFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/book-file")
@Tag(name = "书籍文件关联管理")
public class BookFileController {

    @Autowired
    private BookFileService bookFileService;

    @PostMapping("/save")
    @Operation(summary = "保存书籍文件关联")
    public BaseResponse<Long> saveBookFile(@Validated @RequestBody BookFileDTO bookFileDTO) {
        Long id = bookFileService.saveBookFile(bookFileDTO);
        return ResultUtils.success(id);
    }

    @PostMapping("/saveBatch")
    @Operation(summary = "批量保存书籍文件关联")
    public BaseResponse<Boolean> saveBatchBookFile(@Validated @RequestBody List<BookFileDTO> bookFileDTOList) {
        boolean result = bookFileService.saveBatchBookFile(bookFileDTOList);
        return ResultUtils.success(result);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除书籍文件关联")
    public BaseResponse<Boolean> deleteBookFile(@PathVariable Long id) {
        boolean result = bookFileService.deleteBookFile(id);
        return ResultUtils.success(result);
    }

    @GetMapping("/book/{bookId}")
    @Operation(summary = "获取书籍的文件关联列表")
    public BaseResponse<List<BookFileDTO>> getBookFilesByBookId(@PathVariable Long bookId) {
        List<BookFileDTO> bookFiles = bookFileService.getBookFilesByBookId(bookId);
        return ResultUtils.success(bookFiles);
    }

    @GetMapping("/file/{fileId}")
    @Operation(summary = "获取文件的书籍关联列表")
    public BaseResponse<List<BookFileDTO>> getBookFilesByFileId(@PathVariable Long fileId) {
        List<BookFileDTO> bookFiles = bookFileService.getBookFilesByFileId(fileId);
        return ResultUtils.success(bookFiles);
    }
} 