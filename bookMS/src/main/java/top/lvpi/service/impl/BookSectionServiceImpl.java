package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.lvpi.mapper.BookSectionMapper;
import top.lvpi.mapper.BookMapper;
import top.lvpi.model.entity.BookSection;
import top.lvpi.model.entity.Book;
import top.lvpi.model.entity.Topic;
import top.lvpi.model.es.BookSectionDocument;
import top.lvpi.model.es.TopicLevel;
import top.lvpi.model.dto.topic.TopicPathDTO;
import top.lvpi.service.BookSectionService;
import top.lvpi.service.BookSectionEsService;
import top.lvpi.service.TopicService;
import top.lvpi.common.BusinessException;
import top.lvpi.common.ErrorCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class BookSectionServiceImpl extends ServiceImpl<BookSectionMapper, BookSection> implements BookSectionService {

    private final BookSectionEsService bookSectionEsService;
    private final BookMapper bookMapper;
    private final TopicService topicService;
    
    @Override
    public IPage<BookSection> page(Integer current, Integer size, Long bookId,Integer pageNum, String title, String content) {
        LambdaQueryWrapper<BookSection> wrapper = new LambdaQueryWrapper<>();
        
        // 添加图书ID查询条件
        if (bookId != null) {
            wrapper.eq(BookSection::getBookId, bookId);
        }
        // 添加图书页码查询条件
        if (pageNum != null) {
            wrapper.eq(BookSection::getPageNum, pageNum);
        }
        // 添加文档名称查询条件
        if (StringUtils.isNotBlank(title)) {
            wrapper.like(BookSection::getTitle, title);
        }
        
        // 添加内容模糊查询条件
        if (StringUtils.isNotBlank(content)) {
            wrapper.like(BookSection::getContent, content);
        }
        
        // 按创建时间降序排序
        wrapper.orderByDesc(BookSection::getCreateTime);
        
        return page(new Page<>(current, size), wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithEs(BookSection bookSection) {
        // 1. 更新MySQL数据
        boolean success = updateById(bookSection);
        
        if (success) {
            // 2. 查询完整的图书信息
            Book book = bookMapper.selectById(bookSection.getBookId());
            if (book == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书不存在");
            }
            
            // 3. 获取图书的主题标签
            TopicLevel topicLevels = getBookTopics(book.getId());
            List<Topic> topicTags = topicService.getBookTopicTags(book.getId());
            String[] tags = topicTags.stream()
                .map(Topic::getName)
                .toArray(String[]::new);
            
            // 4. 转换为ES文档并更新
            BookSectionDocument document = new BookSectionDocument();
            document.setId(bookSection.getId().toString());
            document.setBookId(book.getId().toString());
            document.setBookTitle(book.getTitle());
            document.setIsbn(book.getIsbn());
            document.setAuthor(book.getAuthor());
            document.setPublisher(book.getPublisher());
            document.setPageNum(bookSection.getPageNum());
            document.setSectionText(bookSection.getContent());
            document.setFileName(book.getFileName());
            document.setPicUrl(book.getPicUrl());
            document.setPublicationYear(book.getPublicationYear() != null ? Integer.parseInt(book.getPublicationYear()) : null);
            document.setTimestamp(new Date());
            document.setVersion("1");
            document.setTopicLevels(topicLevels);
            document.setTags(tags);
            document.setOpacSeries(book.getOpacSeries());
            document.setSeries(book.getSeries());
            document.setType(book.getType());
            
            bookSectionEsService.saveOrUpdate(document);
        }
        
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteWithEs(Long bookSectionId) {
        // 1. 删除MySQL数据
        boolean success = removeById(bookSectionId);

        if (success) {
            // 2. 同步删除ES索引
            bookSectionEsService.deleteById(String.valueOf(bookSectionId));
        }

        return success;
    }

    // 获取图书的主题标签
    private TopicLevel getBookTopics(Long bookId) {
        // 使用 TopicServiceImpl 中的方法获取与书籍相关的主题
        List<TopicPathDTO> topicsList = topicService.getBookTopicPaths(bookId);
        //区分依据：levelSize区分等级取出path，存入各个等级数组中
        List<String> lvl0 = new ArrayList<>();
        List<String> lvl1 = new ArrayList<>();
        List<String> lvl2 = new ArrayList<>();
        List<String> lvl3 = new ArrayList<>();
        List<String> lvl4 = new ArrayList<>();
        for(TopicPathDTO topicPath : topicsList){
            if(topicPath.getLevelSize() == 0){
                lvl0.add(topicPath.getPath());
            }
            if(topicPath.getLevelSize() == 1){
                lvl1.add(topicPath.getPath());
            }
            if(topicPath.getLevelSize() == 2){
                lvl2.add(topicPath.getPath());
            }
            if(topicPath.getLevelSize() == 3){
                lvl3.add(topicPath.getPath());
            }
            if(topicPath.getLevelSize() == 4){
                lvl4.add(topicPath.getPath());
            }
        }   

        TopicLevel topicLevelObj = new TopicLevel();
        topicLevelObj.setLvl0(lvl0.toArray(new String[0]));
        topicLevelObj.setLvl1(lvl1.toArray(new String[0]));
        topicLevelObj.setLvl2(lvl2.toArray(new String[0]));
        topicLevelObj.setLvl3(lvl3.toArray(new String[0]));
        topicLevelObj.setLvl4(lvl4.toArray(new String[0]));
        return topicLevelObj;
    }
} 