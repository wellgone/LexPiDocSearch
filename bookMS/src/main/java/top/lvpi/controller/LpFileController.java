package top.lvpi.controller;

import top.lvpi.common.BaseResponse;
import top.lvpi.common.ResultUtils;
import top.lvpi.model.dto.file.LpFileDTO;
import top.lvpi.service.LpFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/file")
@Tag(name = "文件信息管理")
public class LpFileController {

    @Autowired
    private LpFileService fileService;

    @PostMapping("/save")
    @Operation(summary = "保存文件信息")
    public BaseResponse<Long> saveFile(@Validated @RequestBody LpFileDTO fileDTO) {
        Long fileId = fileService.saveFile(fileDTO);
        return ResultUtils.success(fileId);
    }

    @PutMapping("/update")
    @Operation(summary = "更新文件信息")
    public BaseResponse<Boolean> updateFile(@Validated @RequestBody LpFileDTO fileDTO) {
        boolean result = fileService.updateFile(fileDTO);
        return ResultUtils.success(result);
    }

    @DeleteMapping("/delete/{fileId}")
    @Operation(summary = "删除文件信息")
    public BaseResponse<Boolean> deleteFile(@PathVariable Long fileId) {
        boolean result = fileService.deleteFile(fileId);
        return ResultUtils.success(result);
    }

    @GetMapping("/get/{fileId}")
    @Operation(summary = "获取文件信息")
    public BaseResponse<LpFileDTO> getFileById(@PathVariable Long fileId) {
        LpFileDTO fileDTO = fileService.getFileById(fileId);
        return ResultUtils.success(fileDTO);
    }

    @GetMapping("/getByMd5/{md5}")
    @Operation(summary = "根据MD5获取文件信息")
    public BaseResponse<LpFileDTO> getFileByMd5(@PathVariable String md5) {
        LpFileDTO fileDTO = fileService.getFileByMd5(md5);
        return ResultUtils.success(fileDTO);
    }
} 