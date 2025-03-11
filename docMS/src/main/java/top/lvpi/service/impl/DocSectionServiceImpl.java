package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.lvpi.mapper.DocSectionMapper;
import top.lvpi.mapper.DocMapper;
import top.lvpi.model.entity.DocSection;
import top.lvpi.model.entity.Doc;
import top.lvpi.model.entity.Topic;
import top.lvpi.model.es.DocSectionDocument;
import top.lvpi.model.es.TopicLevel;
import top.lvpi.model.dto.topic.TopicPathDTO;
import top.lvpi.service.DocSectionService;
import top.lvpi.service.DocSectionEsService;
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
public class DocSectionServiceImpl extends ServiceImpl<DocSectionMapper, DocSection> implements DocSectionService {

    private final DocSectionEsService docSectionEsService;
    private final DocMapper docMapper;
    private final TopicService topicService;
    
    @Override
    public IPage<DocSection> page(Integer current, Integer size, Long docId,Integer pageNum, String title, String content) {
        LambdaQueryWrapper<DocSection> wrapper = new LambdaQueryWrapper<>();
        
        // 添加图书ID查询条件
        if (docId != null) {
            wrapper.eq(DocSection::getDocId, docId);
        }
        // 添加图书页码查询条件
        if (pageNum != null) {
            wrapper.eq(DocSection::getPageNum, pageNum);
        }
        // 添加文档名称查询条件
        if (StringUtils.isNotBlank(title)) {
            wrapper.like(DocSection::getTitle, title);
        }
        
        // 添加内容模糊查询条件
        if (StringUtils.isNotBlank(content)) {
            wrapper.like(DocSection::getContent, content);
        }
        
        // 按创建时间降序排序
        wrapper.orderByDesc(DocSection::getCreateTime);
        
        return page(new Page<>(current, size), wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithEs(DocSection docSection) {
        // 1. 更新MySQL数据
        boolean success = updateById(docSection);
        
        if (success) {
            // 2. 查询完整的图书信息
            Doc doc = docMapper.selectById(docSection.getDocId());
            if (doc == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书不存在");
            }
            
            // 3. 获取图书的主题标签
            TopicLevel topicLevels = getDocTopics(doc.getId());
            List<Topic> topicTags = topicService.getDocTopicTags(doc.getId());
            String[] tags = topicTags.stream()
                .map(Topic::getName)
                .toArray(String[]::new);
            
            // 4. 转换为ES文档并更新
            DocSectionDocument document = new DocSectionDocument();
            document.setId(docSection.getId().toString());
            document.setDocId(doc.getId().toString());
            document.setDocTitle(doc.getTitle());
            document.setIsbn(doc.getIsbn());
            document.setAuthor(doc.getAuthor());
            document.setPublisher(doc.getPublisher());
            document.setPageNum(docSection.getPageNum());
            document.setSectionText(docSection.getContent());
            document.setFileName(doc.getFileName());
            document.setPicUrl(doc.getPicUrl());
            document.setPublicationYear(doc.getPublicationYear() != null ? Integer.parseInt(doc.getPublicationYear()) : null);
            document.setTimestamp(new Date());
            document.setVersion("1");
            document.setTopicLevels(topicLevels);
            document.setTags(tags);
            document.setOpacSeries(doc.getOpacSeries());
            document.setSeries(doc.getSeries());
            document.setType(doc.getType());
            
            docSectionEsService.saveOrUpdate(document);
        }
        
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteWithEs(Long docSectionId) {
        // 1. 删除MySQL数据
        boolean success = removeById(docSectionId);

        if (success) {
            // 2. 同步删除ES索引
            docSectionEsService.deleteById(String.valueOf(docSectionId));
        }

        return success;
    }

    // 获取图书的主题标签
    private TopicLevel getDocTopics(Long docId) {
        // 使用 TopicServiceImpl 中的方法获取与书籍相关的主题
        List<TopicPathDTO> topicsList = topicService.getDocTopicPaths(docId);
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