package top.lvpi.service;

import java.util.List;
import java.util.Map;

/**
 * 统计服务接口
 */
public interface StatisticsService {
    
    /**
     * 获取系统统计数据
     * 
     * @return 包含各项统计数据的Map
     */
    Map<String, Object> getStatistics();
    
    /**
     * 获取书籍总数
     * 
     * @return 书籍总数
     */
    Long getDocCount();
    
    /**
     * 获取已索引书籍数量
     * 
     * @return 已索引书籍数量
     */
    Long getIndexedDocCount();
    
    /**
     * 获取检索报告数量
     * 
     * @return 检索报告数量
     */
    Long getReportCount();
    
    /**
     * 获取笔记数量
     * 
     * @return 笔记数量
     */
    Long getNoteCount();
    
    /**
     * 获取按分类统计的书籍数量
     * 
     * @return 分类统计列表，每项包含category和count
     */
    List<Map<String, Object>> getDocCountByCategory();
    
    /**
     * 获取按丛书名统计的书籍数量
     * 
     * @return 丛书统计列表，每项包含series和count
     */
    List<Map<String, Object>> getDocCountBySeries();

    /**
     * 获取文档总数
     * 
     * @return 文档总数
     */
    Long getDocumentCount();

    /**
     * 获取文件总数
     * 
     * @return 文件总数
     */
    Long getFileCount();    
} 