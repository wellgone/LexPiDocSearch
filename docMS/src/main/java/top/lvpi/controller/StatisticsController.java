package top.lvpi.controller;

import top.lvpi.common.BaseResponse;
import top.lvpi.common.ResultUtils;
import top.lvpi.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 统计控制器
 */
@RestController
@RequestMapping("/statistics")
@Tag(name = "统计管理", description = "系统统计数据相关接口")
public class StatisticsController {
    
    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);
    
    @Autowired
    private StatisticsService statisticsService;
    
    @GetMapping
    @Operation(summary = "获取系统统计数据")
    public BaseResponse<Map<String, Object>> getStatistics() {
        logger.info("获取系统统计数据");
        Map<String, Object> statistics = statisticsService.getStatistics();
        return ResultUtils.success(statistics);
    }
    
    @GetMapping("/document/count")
    @Operation(summary = "获取文档总数")
    public BaseResponse<Long> getDocumentCount() {
        logger.info("获取文档总数");
        Long count = statisticsService.getDocumentCount();
        return ResultUtils.success(count);
    }  

    @GetMapping("/file/count")
    @Operation(summary = "获取文件总数")
    public BaseResponse<Long> getFileCount() {
        logger.info("获取文件总数");
        Long count = statisticsService.getFileCount();
        return ResultUtils.success(count);
    }    

    @GetMapping("/doc/count")
    @Operation(summary = "获取书籍总数")
    public BaseResponse<Long> getDocCount() {
        logger.info("获取书籍总数");
        Long count = statisticsService.getDocCount();
        return ResultUtils.success(count);
    }
    
    @GetMapping("/doc/indexed/count")
    @Operation(summary = "获取已索引书籍数量")
    public BaseResponse<Long> getIndexedDocCount() {
        logger.info("获取已索引书籍数量");
        Long count = statisticsService.getIndexedDocCount();
        return ResultUtils.success(count);
    }
    
    @GetMapping("/indexed/pages/count")
    @Operation(summary = "获取已索引文档页数")
    public BaseResponse<Long> getIndexedPagesCount() {
        logger.info("获取已索引文档页数");
        Long count = statisticsService.getIndexedPagesCount();
        return ResultUtils.success(count);
    }
    
    @GetMapping("/report/count")
    @Operation(summary = "获取检索报告数量")
    public BaseResponse<Long> getReportCount() {
        logger.info("获取检索报告数量");
        Long count = statisticsService.getReportCount();
        return ResultUtils.success(count);
    }
    
    @GetMapping("/note/count")
    @Operation(summary = "获取笔记数量")
    public BaseResponse<Long> getNoteCount() {
        logger.info("获取笔记数量");
        Long count = statisticsService.getNoteCount();
        return ResultUtils.success(count);
    }
    
    @GetMapping("/doc/category/count")
    @Operation(summary = "获取按分类统计的书籍数量")
    public BaseResponse<List<Map<String, Object>>> getDocCountByCategory() {
        logger.info("获取按分类统计的书籍数量");
        List<Map<String, Object>> result = statisticsService.getDocCountByCategory();
        return ResultUtils.success(result);
    }
    
    @GetMapping("/doc/series/count")
    @Operation(summary = "获取按丛书名统计的书籍数量")
    public BaseResponse<List<Map<String, Object>>> getDocCountBySeries() {
        logger.info("获取按丛书名统计的书籍数量");
        List<Map<String, Object>> result = statisticsService.getDocCountBySeries();
        return ResultUtils.success(result);
    }
} 