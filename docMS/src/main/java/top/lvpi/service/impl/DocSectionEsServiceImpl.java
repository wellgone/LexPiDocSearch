package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import top.lvpi.common.BusinessException;
import top.lvpi.common.ErrorCode;
import top.lvpi.mapper.DocMapper;
import top.lvpi.mapper.DocSectionMapper;
import top.lvpi.model.dto.topic.TopicPathDTO;
import top.lvpi.model.entity.Doc;
import top.lvpi.model.entity.DocSection;
import top.lvpi.model.entity.Topic;
import top.lvpi.model.es.DocSectionDocument;
import top.lvpi.model.es.TopicLevel;
import top.lvpi.repository.es.DocSectionRepository;
import top.lvpi.service.DocSectionEsService;
import top.lvpi.service.TopicService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;

@Service
@Slf4j
public class DocSectionEsServiceImpl implements DocSectionEsService {

    @Autowired
    private DocSectionRepository docSectionRepository;

    @Autowired
    private DocSectionMapper docSectionMapper;

    @Autowired
    private DocMapper docMapper;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TopicService topicService;

    @Override
    public void importById(String id) {
        // 实现导入单个文档的逻辑
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importByIsbn(String isbn) {
        if (StringUtils.isBlank(isbn)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "ISBN不能为空");
        }

        // 查询图书信息
        LambdaQueryWrapper<Doc> docQuery = new LambdaQueryWrapper<>();
        docQuery.eq(Doc::getIsbn, isbn);
        Doc doc = docMapper.selectOne(docQuery);
        
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书不存在");
        }

        // 导入图书章节
        importDocSections(doc.getId());

        // 更新图书的索引状态
        LambdaUpdateWrapper<Doc> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Doc::getIsbn, isbn)
                .set(Doc::getIsIndexed, 1);
        docMapper.update(null, updateWrapper);

        log.info("ISBN：{}的图书索引已创建", isbn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importByDocId(Long docId) {
        if (docId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图书ID不能为空");
        }

        // 查询图书信息
        Doc doc = docMapper.selectById(docId);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书不存在");
        }

        // 导入图书章节
        importDocSections(docId);

        // 更新图书的索引状态
        LambdaUpdateWrapper<Doc> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Doc::getId, docId)
                .set(Doc::getIsIndexed, 1);
        docMapper.update(null, updateWrapper);

        log.info("ID：{}的图书索引已创建", docId);
    }

    @Override
    public DocSectionDocument saveOrUpdate(DocSectionDocument document) {
        if (document.getTimestamp() == null) {
            document.setTimestamp(new Date());
        }
        if (document.getVersion() == null) {
            document.setVersion("1");
        }
        return docSectionRepository.save(document);
    }

    @Override
    public void deleteById(String id) {
        docSectionRepository.deleteById(id);
    }

    @Override
    public void deleteByIsbn(String isbn) {
        try {
            // 删除ES中的索引
            DeleteByQueryResponse response = esClient.deleteByQuery(d -> d
                .index("docs")
                .query(q -> q
                    .match(m -> m
                        .field("isbn")
                        .query(isbn)
                    )
                )
                .refresh(true)
            );
            
            log.info("删除索引成功，删除文档数：{}", response.deleted());
            
            // 更新doc表的is_indexed字段为0
            LambdaUpdateWrapper<Doc> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Doc::getIsbn, isbn)
                    .set(Doc::getIsIndexed, 0);
            docMapper.update(null, updateWrapper);

            log.info("ISBN：{}的索引已删除，并更新is_indexed状态", isbn);
        } catch (IOException e) {
            log.error("删除ISBN：{}的索引失败", isbn, e);
            throw new RuntimeException("删除索引失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByDocId(String docId) {
        try {
            // 1. 删除ES中的索引
            DeleteByQueryResponse response = esClient.deleteByQuery(d -> d
                .index("docs")
                .query(q -> q
                    .match(m -> m
                        .field("book_id")
                        .query(docId)
                    )
                )
                .refresh(true)
            );
            
            log.info("删除索引成功，删除文档数：{}", response.deleted());

            // 2. 更新doc表的is_indexed字段为0
            LambdaUpdateWrapper<Doc> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Doc::getId, docId)
                    .set(Doc::getIsIndexed, 0);
            docMapper.update(null, updateWrapper);

            log.info("书籍ID：{}的索引已删除，并更新is_indexed状态", docId);
        } catch (IOException e) {
            log.error("删除书籍ID：{}的索引失败", docId, e);
            throw new RuntimeException("删除索引失败：" + e.getMessage());
        }
    }

    @Override
    public Page<DocSectionDocument> search(String keyword, String isbn, String docId, String docTitle,
            String author, String publisher, Integer pageNum, Pageable pageable) {
        // 构建查询条件
        Criteria criteria = new Criteria();

        // 添加各个字段的查询条件
        if (StringUtils.isNotBlank(keyword)) {
            // 在文本和主题名称中搜索关键词
            Criteria keywordCriteria = new Criteria();
            keywordCriteria.or(new Criteria("section_text").matches(keyword))
                          .or(new Criteria("topics.name").matches(keyword));
            criteria.and(keywordCriteria);
        }

        if (StringUtils.isNotBlank(isbn)) {
            criteria.and(new Criteria("isbn").is(isbn));
        }

        if (StringUtils.isNotBlank(docId)) {
            criteria.and(new Criteria("book_id").is(docId));
        }

        if (StringUtils.isNotBlank(docTitle)) {
            criteria.and(new Criteria("book_title").matches(docTitle));
        }

        if (StringUtils.isNotBlank(author)) {
            criteria.and(new Criteria("author").matches(author));
        }

        if (StringUtils.isNotBlank(publisher)) {
            criteria.and(new Criteria("publisher").matches(publisher));
        }

        if (pageNum != null) {
            criteria.and(new Criteria("page_num").is(pageNum.toString()));
        }

        // 创建查询对象
        CriteriaQuery query = new CriteriaQuery(criteria).setPageable(pageable);

        // 执行查询
        SearchHits<DocSectionDocument> searchHits = elasticsearchOperations.search(query, DocSectionDocument.class);

        // 转换结果
        List<DocSectionDocument> content = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        // 返回分页结果
        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }

    @Override
    public void importDocSections(Long docId) {
        // 查询图书的所有章节
        LambdaQueryWrapper<DocSection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocSection::getDocId, docId);
        List<DocSection> sections = docSectionMapper.selectList(queryWrapper);
        
        if (sections.isEmpty()) {
            return;
        }

        // 获取图书信息
        Doc doc = docMapper.selectById(docId);
        if (doc == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书不存在");
        }

        // 获取图书的主题标签
        TopicLevel topicLevels = getDocTopics(docId);
        List<Topic> topicTags = topicService.getDocTopicTags(docId);
        //提取topicTags中的name字段，存入tags[]数组
        String[] tags = topicTags.stream()
            .map(Topic::getName)
            .toArray(String[]::new);

        // 批量转换并保存到ES
        List<DocSectionDocument> documents = sections.stream()
            .map(section -> {
                DocSectionDocument document = new DocSectionDocument();
                document.setId(section.getId().toString());
                document.setDocId(docId.toString());
                document.setDocTitle(doc.getTitle());
                document.setIsbn(doc.getIsbn());
                document.setAuthor(doc.getAuthor());
                document.setPublisher(doc.getPublisher());
                document.setPageNum(section.getPageNum());
                document.setSectionText(section.getContent());
                document.setFileName(doc.getFileName());
                document.setPicUrl(doc.getPicUrl());
                //将字符串转化为Integer
                document.setPublicationYear(doc.getPublicationYear() != null ? Integer.parseInt(doc.getPublicationYear()) : null);
                document.setTimestamp(new Date());
                document.setVersion("1");
                document.setTopicLevels(topicLevels);  // 设置主题标签
                document.setTags(tags);
                document.setOpacSeries(doc.getOpacSeries());
                document.setSeries(doc.getSeries());
                document.setCategory(doc.getCategory());
                document.setType(doc.getType());
                return document;
            })
            .collect(Collectors.toList());

        docSectionRepository.saveAll(documents);
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