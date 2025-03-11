package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import top.lvpi.common.BusinessException;
import top.lvpi.common.ErrorCode;
import top.lvpi.mapper.BookMapper;
import top.lvpi.mapper.BookSectionMapper;
import top.lvpi.model.dto.topic.TopicPathDTO;
import top.lvpi.model.entity.Book;
import top.lvpi.model.entity.BookSection;
import top.lvpi.model.entity.Topic;
import top.lvpi.model.es.BookSectionDocument;
import top.lvpi.model.es.TopicLevel;
import top.lvpi.repository.es.BookSectionRepository;
import top.lvpi.service.BookSectionEsService;
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
public class BookSectionEsServiceImpl implements BookSectionEsService {

    @Autowired
    private BookSectionRepository bookSectionRepository;

    @Autowired
    private BookSectionMapper bookSectionMapper;

    @Autowired
    private BookMapper bookMapper;

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
        LambdaQueryWrapper<Book> bookQuery = new LambdaQueryWrapper<>();
        bookQuery.eq(Book::getIsbn, isbn);
        Book book = bookMapper.selectOne(bookQuery);
        
        if (book == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书不存在");
        }

        // 导入图书章节
        importBookSections(book.getId());

        // 更新图书的索引状态
        LambdaUpdateWrapper<Book> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Book::getIsbn, isbn)
                .set(Book::getIsIndexed, 1);
        bookMapper.update(null, updateWrapper);

        log.info("ISBN：{}的图书索引已创建", isbn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importByBookId(Long bookId) {
        if (bookId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图书ID不能为空");
        }

        // 查询图书信息
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书不存在");
        }

        // 导入图书章节
        importBookSections(bookId);

        // 更新图书的索引状态
        LambdaUpdateWrapper<Book> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Book::getId, bookId)
                .set(Book::getIsIndexed, 1);
        bookMapper.update(null, updateWrapper);

        log.info("ID：{}的图书索引已创建", bookId);
    }

    @Override
    public BookSectionDocument saveOrUpdate(BookSectionDocument document) {
        if (document.getTimestamp() == null) {
            document.setTimestamp(new Date());
        }
        if (document.getVersion() == null) {
            document.setVersion("1");
        }
        return bookSectionRepository.save(document);
    }

    @Override
    public void deleteById(String id) {
        bookSectionRepository.deleteById(id);
    }

    @Override
    public void deleteByIsbn(String isbn) {
        try {
            // 删除ES中的索引
            DeleteByQueryResponse response = esClient.deleteByQuery(d -> d
                .index("books")
                .query(q -> q
                    .match(m -> m
                        .field("isbn")
                        .query(isbn)
                    )
                )
                .refresh(true)
            );
            
            log.info("删除索引成功，删除文档数：{}", response.deleted());
            
            // 更新book表的is_indexed字段为0
            LambdaUpdateWrapper<Book> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Book::getIsbn, isbn)
                    .set(Book::getIsIndexed, 0);
            bookMapper.update(null, updateWrapper);

            log.info("ISBN：{}的索引已删除，并更新is_indexed状态", isbn);
        } catch (IOException e) {
            log.error("删除ISBN：{}的索引失败", isbn, e);
            throw new RuntimeException("删除索引失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByBookId(String bookId) {
        try {
            // 1. 删除ES中的索引
            DeleteByQueryResponse response = esClient.deleteByQuery(d -> d
                .index("books")
                .query(q -> q
                    .match(m -> m
                        .field("book_id")
                        .query(bookId)
                    )
                )
                .refresh(true)
            );
            
            log.info("删除索引成功，删除文档数：{}", response.deleted());

            // 2. 更新book表的is_indexed字段为0
            LambdaUpdateWrapper<Book> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Book::getId, bookId)
                    .set(Book::getIsIndexed, 0);
            bookMapper.update(null, updateWrapper);

            log.info("书籍ID：{}的索引已删除，并更新is_indexed状态", bookId);
        } catch (IOException e) {
            log.error("删除书籍ID：{}的索引失败", bookId, e);
            throw new RuntimeException("删除索引失败：" + e.getMessage());
        }
    }

    @Override
    public Page<BookSectionDocument> search(String keyword, String isbn, String bookId, String bookTitle,
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

        if (StringUtils.isNotBlank(bookId)) {
            criteria.and(new Criteria("book_id").is(bookId));
        }

        if (StringUtils.isNotBlank(bookTitle)) {
            criteria.and(new Criteria("book_title").matches(bookTitle));
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
        SearchHits<BookSectionDocument> searchHits = elasticsearchOperations.search(query, BookSectionDocument.class);

        // 转换结果
        List<BookSectionDocument> content = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        // 返回分页结果
        return new PageImpl<>(content, pageable, searchHits.getTotalHits());
    }

    @Override
    public void importBookSections(Long bookId) {
        // 查询图书的所有章节
        LambdaQueryWrapper<BookSection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BookSection::getBookId, bookId);
        List<BookSection> sections = bookSectionMapper.selectList(queryWrapper);
        
        if (sections.isEmpty()) {
            return;
        }

        // 获取图书信息
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图书不存在");
        }

        // 获取图书的主题标签
        TopicLevel topicLevels = getBookTopics(bookId);
        List<Topic> topicTags = topicService.getBookTopicTags(bookId);
        //提取topicTags中的name字段，存入tags[]数组
        String[] tags = topicTags.stream()
            .map(Topic::getName)
            .toArray(String[]::new);

        // 批量转换并保存到ES
        List<BookSectionDocument> documents = sections.stream()
            .map(section -> {
                BookSectionDocument document = new BookSectionDocument();
                document.setId(section.getId().toString());
                document.setBookId(bookId.toString());
                document.setBookTitle(book.getTitle());
                document.setIsbn(book.getIsbn());
                document.setAuthor(book.getAuthor());
                document.setPublisher(book.getPublisher());
                document.setPageNum(section.getPageNum());
                document.setSectionText(section.getContent());
                document.setFileName(book.getFileName());
                document.setPicUrl(book.getPicUrl());
                //将字符串转化为Integer
                document.setPublicationYear(book.getPublicationYear() != null ? Integer.parseInt(book.getPublicationYear()) : null);
                document.setTimestamp(new Date());
                document.setVersion("1");
                document.setTopicLevels(topicLevels);  // 设置主题标签
                document.setTags(tags);
                document.setOpacSeries(book.getOpacSeries());
                document.setSeries(book.getSeries());
                document.setCategory(book.getCategory());
                document.setType(book.getType());
                return document;
            })
            .collect(Collectors.toList());

        bookSectionRepository.saveAll(documents);
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