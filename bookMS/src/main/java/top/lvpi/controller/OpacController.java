package top.lvpi.controller;

import top.lvpi.common.BaseResponse;
import top.lvpi.common.ResultUtils;
import top.lvpi.service.BookService;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/opac")
@Tag(name = "OPAC管理", description = "OPAC相关接口")
public class OpacController {

    @Resource
    private BookService bookService;

    // 用于存储任务状态
    private static final Map<String, String> taskStatusMap = new ConcurrentHashMap<>();

    @GetMapping("/{id}")
    @Operation(summary = "获取OPAC信息")
    public BaseResponse<String> getOpacInfo(@PathVariable Long id) {
        String result = bookService.getAndSaveOpacInfo(id);
        return ResultUtils.success(result);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量获取OPAC信息")
    public BaseResponse<String> batchGetOpacInfo() {
        String taskId = UUID.randomUUID().toString();
        CompletableFuture.runAsync(() -> {
            String result = bookService.batchGetAndSaveOpacInfo();
            taskStatusMap.put(taskId, result);
        });
        return ResultUtils.success(taskId);
    }

    @GetMapping("/batch/status/{taskId}")
    @Operation(summary = "获取批量任务状态")
    public BaseResponse<String> getBatchStatus(@PathVariable String taskId) {
        String status = taskStatusMap.get(taskId);
        if (status == null) {
            return ResultUtils.success("任务正在处理中...");
        }
        // 任务完成后，从Map中移除状态
        taskStatusMap.remove(taskId);
        return ResultUtils.success(status);
    }
} 