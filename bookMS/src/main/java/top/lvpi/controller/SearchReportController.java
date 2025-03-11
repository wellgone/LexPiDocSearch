package top.lvpi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import top.lvpi.common.BaseResponse;
import top.lvpi.common.ErrorCode;
import top.lvpi.model.entity.Note;
import top.lvpi.model.entity.SearchReport;
import top.lvpi.service.SearchReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.stp.StpUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/report")
@Tag(name = "检索报告管理", description = "检索报告相关接口")
@Slf4j
public class SearchReportController {

    @Autowired
    private SearchReportService reportService;

    @Operation(summary = "创建检索报告")
    @PostMapping("/create")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> createReport(@RequestBody SearchReport report) {
        boolean result = reportService.createReport(report);
        return BaseResponse.success(result);
    }

    @Operation(summary = "更新检索报告")
    @PutMapping("/update")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> updateReport(@RequestBody SearchReport report) {
        boolean result = reportService.updateReport(report);
        return BaseResponse.success(result);
    }

    @Operation(summary = "删除检索报告")
    @DeleteMapping("/delete/{id}")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> deleteReport(@PathVariable Long id) {
        boolean result = reportService.deleteReport(id);
        return BaseResponse.success(result);
    }

    @Operation(summary = "获取检索报告详情")
    @GetMapping("/get/{id}")
    public BaseResponse<SearchReport> getReportById(@PathVariable Long id) {
        SearchReport report = reportService.getReportById(id);
        return BaseResponse.success(report);
    }

    @Operation(summary = "分页查询检索报告列表")
    @GetMapping("/list")
    public BaseResponse<IPage<SearchReport>> listReports(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        IPage<SearchReport> page = reportService.listReports(current, size, keyword, userId);
        return BaseResponse.success(page);
    }

    @Operation(summary = "获取报告关联的笔记列表")
    @GetMapping("/{reportId}/notes")
    public BaseResponse<List<Note>> getReportNotes(@PathVariable Long reportId) {
        List<Note> notes = reportService.getReportNotes(reportId);
        return BaseResponse.success(notes);
    }

    @Operation(summary = "导出报告为PDF")
    @GetMapping("/{reportId}/export/pdf")
    public ResponseEntity<byte[]> exportReportToPdf(@PathVariable Long reportId) {
        byte[] pdfData = reportService.exportReportToPdf(reportId);
        SearchReport report = reportService.getReportById(reportId);
        String filename = report.getTitle() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        try {
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            headers.add("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");
        } catch (UnsupportedEncodingException e) {
            log.error("文件名编码失败", e);
            headers.setContentDispositionFormData("attachment", "report.pdf");
        }
        headers.setContentLength(pdfData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);
    }

    @Operation(summary = "导出报告为Markdown")
    @GetMapping("/{reportId}/export/markdown")
    public ResponseEntity<String> exportReportToMarkdown(@PathVariable Long reportId) {
        String markdown = reportService.exportReportToMarkdown(reportId);
        SearchReport report = reportService.getReportById(reportId);
        String filename = report.getTitle() + ".md";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_MARKDOWN);
        try {
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            headers.add("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");
        } catch (UnsupportedEncodingException e) {
            log.error("文件名编码失败", e);
            headers.setContentDispositionFormData("attachment", "report.md");
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(markdown);
    }

    @Operation(summary = "生成报告分享链接")
    @PostMapping("/{reportId}/share")
    @SaCheckRole("admin")
    public BaseResponse<String> generateShareLink(@PathVariable Long reportId) {
        String shareLink = reportService.generateShareLink(reportId);
        return BaseResponse.success(shareLink);
    }

    @Operation(summary = "获取用户的报告列表")
    @GetMapping("/user/{userId}")
    public BaseResponse<List<SearchReport>> getUserReports(@PathVariable Long userId) {
        List<SearchReport> reports = reportService.getUserReports(userId);
        return BaseResponse.success(reports);
    }

    @Operation(summary = "获取分享的报告")
    @GetMapping("/share/{fileName}")
    public ResponseEntity<byte[]> getSharedReport(@PathVariable String fileName) {
        byte[] pdfData = reportService.getSharedReport(fileName);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdfData.length);
        headers.add("Content-Disposition", "inline; filename=\"shared_report.pdf\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfData);
    }

    @Operation(summary = "获取当前用户的检索菜单报告列表")
    @GetMapping("/search-subjects")
    public BaseResponse<List<SearchReport>> getSearchSubjectReports() {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        List<SearchReport> reports = reportService.getSearchSubjectReports(userId);
        return BaseResponse.success(reports);
    }
} 