package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import top.lvpi.mapper.BookMapper;
import top.lvpi.mapper.NoteMapper;
import top.lvpi.mapper.SearchReportMapper;
import top.lvpi.model.entity.Book;
import top.lvpi.service.StatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务实现类
 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    @Autowired
    private BookMapper bookMapper;
    
    @Autowired
    private NoteMapper noteMapper;
    
    @Autowired
    private SearchReportMapper searchReportMapper;
    
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 获取系统统计数据
     * 
     * @return 包含各项统计数据的Map
     */
    @Override
    public Map<String, Object> getStatistics() {    
        Map<String, Object> stats = new HashMap<>();
        
        // 从Spring容器中获取当前bean的代理对象，确保@DS注解生效
        StatisticsService self = applicationContext.getBean(StatisticsService.class);
        
        try {
            stats.put("bookCount", self.getBookCount());
        } catch (Exception e) {
            logger.error("获取书籍总数失败", e);
            stats.put("bookCount", 0L);
        }

        try {   
            stats.put("documentCount", self.getDocumentCount());
        } catch (Exception e) {
            logger.error("获取文档总数失败", e);
            stats.put("documentCount", 0L);
        }

        try {   
            stats.put("fileCount", self.getFileCount());
        } catch (Exception e) {
            logger.error("获取文件总数失败", e);
            stats.put("fileCount", 0L);
        }

        try {
            stats.put("indexedBookCount", self.getIndexedBookCount());
        } catch (Exception e) {
            logger.error("获取已索引书籍数量失败", e);
            stats.put("indexedBookCount", 0L);
        }
        
        try {
            stats.put("reportCount", self.getReportCount());
        } catch (Exception e) {
            logger.error("获取检索报告数量失败", e);
            stats.put("reportCount", 0L);
        }
        
        try {
            stats.put("noteCount", self.getNoteCount());
        } catch (Exception e) {
            logger.error("获取笔记数量失败", e);
            stats.put("noteCount", 0L);
        }
        
        return stats;
    }

    /**
     * 获取书籍总数
     * 
     * @return 书籍总数
     */
    @Override
    public Long getBookCount() {
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Book::getType, 1);
        return bookMapper.selectCount(queryWrapper);    
    }

    /**
     * 获取文档总数
     * 
     * @return 文档总数
     */
    @Override
    public Long getDocumentCount() {
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Book::getType, 2);
        return bookMapper.selectCount(queryWrapper);
    }   

    /**
     * 获取文件总数
     * 
     * @return 文件总数
     */
    @Override
    public Long getFileCount() {
        return bookMapper.selectCount(null);
    }

    /**
     * 获取已索引书籍数量
     * 
     * @return 已索引书籍数量
     */
    @Override
    public Long getIndexedBookCount() {
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Book::getIsIndexed, 1);
        return bookMapper.selectCount(queryWrapper);
    }

    /**
     * 获取检索报告数量
     * 
     * @return 检索报告数量
     */ 
    @Override
    public Long getReportCount() {
        return searchReportMapper.selectCount(null);
    }

    /**
     * 获取笔记数量
     * 
     * @return 笔记数量
     */
    @Override
    public Long getNoteCount() {
        return noteMapper.selectCount(null);
    }

    /**
     * 获取按分类统计的书籍数量
     * 
     * @return 分类统计列表，每项包含category和count
     */
    @Override
    public List<Map<String, Object>> getBookCountByCategory() {
        try {
            return bookMapper.selectBookCountByCategory();
        } catch (Exception e) {
            logger.error("获取按分类统计的书籍数量失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取按丛书名统计的书籍数量
     * 
     * @return 丛书统计列表，每项包含series和count
     */
    @Override
    public List<Map<String, Object>> getBookCountBySeries() {
        try {
            return bookMapper.selectBookCountBySeries();
        } catch (Exception e) {
            logger.error("获取按丛书名统计的书籍数量失败", e);
            return Collections.emptyList();
        }
    }

    
} 