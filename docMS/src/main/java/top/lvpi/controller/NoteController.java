package top.lvpi.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import top.lvpi.common.BaseResponse;
import top.lvpi.common.ErrorCode;
import top.lvpi.model.entity.Note;
import top.lvpi.model.request.NoteHierarchyRequest;
import top.lvpi.model.request.BatchNoteHierarchyRequest;
import top.lvpi.model.request.BatchNoteOrderRequest;
import top.lvpi.model.request.CreateNoteRequest;
import top.lvpi.model.request.BatchDeleteNoteRequest;
import top.lvpi.model.vo.NoteVO;
import top.lvpi.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/note")
@Tag(name = "笔记管理", description = "笔记相关接口")
@Slf4j
public class NoteController {

    @Autowired
    private NoteService noteService;

    @Operation(summary = "创建笔记")
    @PostMapping("/create")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> createNote(@RequestBody CreateNoteRequest request) {
        boolean result = noteService.createNote(request.getNote(), request.getReportId());
        return BaseResponse.success(result);
    }

    @Operation(summary = "更新笔记")
    @PutMapping("/update")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> updateNote(@RequestBody Note note) {
        boolean result = noteService.updateNote(note);
        return BaseResponse.success(result);
    }

    @Operation(summary = "删除笔记")
    @DeleteMapping("/delete/{id}")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> deleteNote(@PathVariable Long id) {
        boolean result = noteService.deleteNote(id);
        return BaseResponse.success(result);
    }

    @Operation(summary = "获取笔记详情")
    @GetMapping("/get/{id}")
    public BaseResponse<Note> getNoteById(@PathVariable Long id) {
        Note note = noteService.getNoteById(id);
        return BaseResponse.success(note);
    }

    @Operation(summary = "分页查询笔记列表")
    @GetMapping("/list")
    public BaseResponse<IPage<Note>> listNotes(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        IPage<Note> page = noteService.listNotes(current, size, keyword);
        return BaseResponse.success(page);
    }

    @Operation(summary = "按来源搜索笔记")
    @GetMapping("/search/source")
    public BaseResponse<IPage<Note>> searchBySource(
            @Parameter(description = "来源名称") @RequestParam String sourceName,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        IPage<Note> page = noteService.searchBySource(sourceName, current, size);
        return BaseResponse.success(page);
    }

    @Operation(summary = "按标签搜索笔记")
    @GetMapping("/search/tags")
    public BaseResponse<IPage<Note>> searchByTags(
            @Parameter(description = "标签") @RequestParam String tags,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        IPage<Note> page = noteService.searchByTags(tags, current, size);
        return BaseResponse.success(page);
    }

    @Operation(summary = "按时间范围搜索笔记")
    @GetMapping("/search/time")
    public BaseResponse<IPage<Note>> searchByTimeRange(
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {
        IPage<Note> page = noteService.searchByTimeRange(startTime, endTime, current, size);
        return BaseResponse.success(page);
    }

    @Operation(summary = "获取用户笔记列表")
    @GetMapping("/user/{userId}")
    public BaseResponse<List<Note>> getUserNotes(@PathVariable Long userId) {
        List<Note> notes = noteService.getUserNotes(userId);
        return BaseResponse.success(notes);
    }

    @Operation(summary = "获取报告的树形结构笔记")
    @GetMapping("/report/{reportId}/tree")
    public BaseResponse<List<NoteVO>> getReportNoteTree(@PathVariable Long reportId) {
        List<NoteVO> notes = noteService.getReportNoteTree(reportId);
        return BaseResponse.success(notes);
    }

    @Operation(summary = "更新笔记层级关系")
    @PutMapping("/hierarchy")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> updateNoteHierarchy(
            @RequestBody NoteHierarchyRequest request) {
        boolean result = noteService.updateNoteHierarchy(
            request.getNoteId(), 
            request.getParentId(), 
            request.getOrderNum()
        );
        return BaseResponse.success(result);
    }

    @Operation(summary = "批量更新笔记层级关系")
    @PutMapping("/hierarchy/batch")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> batchUpdateNoteHierarchy(
            @RequestBody BatchNoteHierarchyRequest request) {
        boolean result = noteService.batchUpdateNoteHierarchy(request.getNotes());
        return BaseResponse.success(result);
    }

    @Operation(summary = "批量更新笔记排序")
    @PutMapping("/order/batch")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> batchUpdateNoteOrder(
            @RequestBody BatchNoteOrderRequest request) {
        boolean result = noteService.batchUpdateNoteOrder(request.getNotes());
        return BaseResponse.success(result);
    }

    @Operation(summary = "关联笔记到报告")
    @PostMapping("/relate/{noteId}/report/{reportId}")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> relateToReport(
            @PathVariable Long noteId,
            @PathVariable Long reportId) {
        boolean result = noteService.relateToReport(noteId, reportId);
        return BaseResponse.success(result);
    }

    @Operation(summary = "从报告中移除笔记")
    @DeleteMapping("/unrelate/{noteId}/report/{reportId}")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> removeFromReport(
            @PathVariable Long noteId,
            @PathVariable Long reportId) {
        boolean result = noteService.removeFromReport(noteId, reportId);
        return BaseResponse.success(result);
    }

    @Operation(summary = "批量删除笔记")
    @DeleteMapping("/batch/delete")
    @SaCheckRole("admin")
    public BaseResponse<Boolean> batchDeleteNotes(@RequestBody BatchDeleteNoteRequest request) {
        boolean result = noteService.batchDeleteNotes(request.getNoteIds());
        return BaseResponse.success(result);
    }
} 