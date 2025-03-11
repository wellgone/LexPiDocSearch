package top.lvpi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import top.lvpi.model.entity.Note;
import top.lvpi.model.request.BatchNoteOrderRequest.NoteOrder;
import top.lvpi.model.request.BatchNoteHierarchyRequest.NoteHierarchy;
import top.lvpi.model.vo.NoteVO;

import java.util.List;

public interface NoteService extends IService<Note> {
    /**
     * 创建笔记
     */
    boolean createNote(Note note, Long reportId);

    /**
     * 更新笔记
     */
    boolean updateNote(Note note);

    /**
     * 删除笔记
     */
    boolean deleteNote(Long id);

    /**
     * 获取笔记详情
     */
    Note getNoteById(Long id);

    /**
     * 分页查询笔记列表
     */
    IPage<Note> listNotes(int current, int size, String keyword);

    /**
     * 按来源搜索笔记
     */
    IPage<Note> searchBySource(String sourceName, int current, int size);

    /**
     * 按标签搜索笔记
     */
    IPage<Note> searchByTags(String tags, int current, int size);

    /**
     * 按时间范围搜索笔记
     */
    IPage<Note> searchByTimeRange(String startTime, String endTime, int current, int size);

    /**
     * 获取用户的所有笔记
     */
    List<Note> getUserNotes(Long userId);

    /**
     * 关联笔记到报告
     */
    boolean relateToReport(Long noteId, Long reportId);

    /**
     * 从报告中移除笔记
     */
    boolean removeFromReport(Long noteId, Long reportId);

    /**
     * 获取报告的树形结构笔记
     */
    List<NoteVO> getReportNoteTree(Long reportId);

    /**
     * 更新笔记层级关系
     */
    boolean updateNoteHierarchy(Long noteId, Long parentId, Integer orderNum);

    /**
     * 批量更新笔记排序
     */
    boolean batchUpdateNoteOrder(List<NoteOrder> notes);

    /**
     * 批量更新笔记层级关系
     */
    boolean batchUpdateNoteHierarchy(List<NoteHierarchy> notes);

    /**
     * 批量删除笔记
     */
    boolean batchDeleteNotes(List<Long> noteIds);
} 