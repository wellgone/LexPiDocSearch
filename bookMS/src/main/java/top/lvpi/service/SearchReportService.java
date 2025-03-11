package top.lvpi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import top.lvpi.model.entity.Note;
import top.lvpi.model.entity.SearchReport;

import java.util.List;

public interface SearchReportService extends IService<SearchReport> {
    /**
     * 创建检索报告
     */
    boolean createReport(SearchReport report);

    /**
     * 更新检索报告
     */
    boolean updateReport(SearchReport report);

    /**
     * 删除检索报告
     */
    boolean deleteReport(Long id);

    /**
     * 获取检索报告详情
     */
    SearchReport getReportById(Long id);

    /**
     * 分页查询检索报告列表
     * @param current 页码
     * @param size 每页大小
     * @param keyword 搜索关键词
     * @param userId 用户ID
     * @return 分页结果
     */
    IPage<SearchReport> listReports(int current, int size, String keyword, Long userId);

    /**
     * 获取报告关联的笔记列表
     */
    List<Note> getReportNotes(Long reportId);

    /**
     * 导出报告为PDF
     */
    byte[] exportReportToPdf(Long reportId);

    /**
     * 导出报告为Markdown
     */
    String exportReportToMarkdown(Long reportId);

    /**
     * 生成报告分享链接
     */
    String generateShareLink(Long reportId);

    byte[] getSharedReport(String fileName);

    /**
     * 获取用户的所有报告
     */
    List<SearchReport> getUserReports(Long userId);

    /**
     * 获取用户的检索菜单报告列表
     * @param userId 用户ID
     * @return 检索菜单报告列表
     */
    List<SearchReport> getSearchSubjectReports(Long userId);
} 