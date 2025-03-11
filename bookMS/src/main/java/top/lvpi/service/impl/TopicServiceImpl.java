package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.lvpi.mapper.TopicMapper;
import top.lvpi.mapper.BookTopicMapper;
import top.lvpi.model.dto.topic.TopicImportDTO;
import top.lvpi.model.dto.topic.TopicPathDTO;
import top.lvpi.model.dto.topic.TopicTreeDTO;
import top.lvpi.model.entity.Topic;
import top.lvpi.model.entity.BookTopic;
import top.lvpi.service.TopicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {
    private static final Logger logger = LoggerFactory.getLogger(TopicServiceImpl.class);

    @Autowired
    private BookTopicMapper bookTopicMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Topic topic) {
        // 如果是不带层级的标签（parentId为null），则验证level值
        if (topic.getParentId() == null) {
            // 如果level为null，设置默认值100
            if (topic.getLevel() == null) {
                topic.setLevel(100);
            }
        }
        
        boolean result = super.save(topic);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(Topic topic) {
        // 如果是不带层级的标签（parentId为null），则验证level值
        if (topic.getParentId() == null) {
            // 如果level为null，设置默认值100
            if (topic.getLevel() == null) {
                topic.setLevel(100);
            }
        }
        
        boolean result = super.updateById(topic);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        boolean result = super.removeById(id);

        return result;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long importTopics(TopicImportDTO topicImportDTO) {
        if (topicImportDTO == null || !StringUtils.hasText(topicImportDTO.getTitle())) {
            throw new IllegalArgumentException("主题名称不能为空");
        }
        // 清理主题名称
        topicImportDTO.setTitle(cleanString(topicImportDTO.getTitle()));
        return importTopicWithParent(topicImportDTO, null, 0);
    }

    /**
     * 递归导入主题及其子主题
     * @param importDTO 导入数据
     * @param parentId 父主题ID
     * @param level 当前层级
     * @return 导入的主题ID
     */
    private Long importTopicWithParent(TopicImportDTO importDTO, Long parentId, Integer level) {
        // 1. 创建当前主题
        Topic topic = new Topic();
        topic.setName(importDTO.getTitle());
        topic.setParentId(parentId);
        topic.setLevel(level);
        this.save(topic);

        // 2. 递归处理子主题
        if (importDTO.getChildren() != null && !importDTO.getChildren().isEmpty()) {
            for (TopicImportDTO child : importDTO.getChildren()) {
                // 清理子主题名称
                child.setTitle(cleanString(child.getTitle()));
                importTopicWithParent(child, topic.getId(), level + 1);
            }
        }

        return topic.getId();
    }

    /**
     * 清理字符串中的<EOL>和多余空格
     * @param str 原始字符串
     * @return 清理后的字符串
     */
    private String cleanString(String str) {
        if (str == null) {
            return null;
        }
        // 1. 移除<EOL>
        str = str.replace("<EOL>", "");
        // 2. 移除首尾空格
        str = str.trim();
        // 3. 将连续的空格替换为单个空格
        str = str.replaceAll("\\s+", " ");
        return str;
    }

    @Override
    public List<Topic> getTopicsByBookId(Long bookId) {
        // 1. 获取书籍关联的所有主题ID
        LambdaQueryWrapper<BookTopic> bookTopicWrapper = new LambdaQueryWrapper<>();
        bookTopicWrapper.eq(BookTopic::getBookId, bookId)
                .eq(BookTopic::getIsDeleted, 0);
        List<BookTopic> bookTopics = bookTopicMapper.selectList(bookTopicWrapper);
        
        if (bookTopics.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 获取这些主题的详细信息
        Set<Long> topicIds = bookTopics.stream()
                .map(BookTopic::getTopicId)
                .collect(Collectors.toSet());

        // 3. 查询这些主题及其所有父主题
        Set<Long> allTopicIds = new HashSet<>(topicIds);
        List<Topic> topics = this.listByIds(topicIds);
        
        // 递归获取所有父主题
        for (Topic topic : topics) {
            Long parentId = topic.getParentId();
            while (parentId != null) {
                allTopicIds.add(parentId);
                Topic parentTopic = this.getById(parentId);
                if (parentTopic != null) {
                    parentId = parentTopic.getParentId();
                } else {
                    break;
                }
            }
        }
        List<Topic> topicsResult = this.listByIds(allTopicIds);
        //剔除parent_id、level为null的
        topicsResult.removeIf(topic -> topic.getParentId() == null || topic.getLevel() == null);
        // 4. 返回所有相关主题
        return topicsResult;
    }

    @Override
    public List<TopicTreeDTO> getTopicTreeByBookId(Long bookId) {
        // 1. 获取所有相关主题
        List<Topic> allTopics = getTopicsByBookId(bookId);
        if (allTopics.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 过滤掉parentId为空且level大于等于100的主题
        allTopics = allTopics.stream()
                .filter(topic -> topic.getParentId() != null && topic.getLevel() != null && topic.getLevel() < 100)
                .collect(Collectors.toList());

        // 3. 转换为TopicTreeDTO
        List<TopicTreeDTO> allTopicTrees = allTopics.stream()
                .map(this::convertToTreeDTO)
                .collect(Collectors.toList());

        // 4. 构建主题ID到DTO的映射，方便查找
        Map<Long, TopicTreeDTO> topicMap = allTopicTrees.stream()
                .collect(Collectors.toMap(TopicTreeDTO::getId, dto -> dto));

        // 5. 构建树形结构
        List<TopicTreeDTO> rootTopics = new ArrayList<>();
        for (TopicTreeDTO topic : allTopicTrees) {
            Long parentId = topic.getParentId();
            if (parentId == null || parentId == 0) {
                // 这是一个根节点
                rootTopics.add(topic);
            } else {
                // 将当前节点添加到父节点的children列表中
                TopicTreeDTO parentTopic = topicMap.get(parentId);
                if (parentTopic != null) {
                    if (parentTopic.getChildren() == null) {
                        parentTopic.setChildren(new ArrayList<>());
                    }
                    parentTopic.getChildren().add(topic);
                }
            }
        }

        return rootTopics;
    }

    private TopicTreeDTO convertToTreeDTO(Topic topic) {
        TopicTreeDTO dto = new TopicTreeDTO();
        BeanUtils.copyProperties(topic, dto);
        return dto;
    }

    @Override
    public List<Topic> getTopicWithParents(Long topicId) {
        List<Topic> result = new ArrayList<>();
        
        // 获取当前主题
        Topic currentTopic = this.getById(topicId);
        if (currentTopic == null || currentTopic.getParentId() == null || currentTopic.getLevel() == null) {
            return result;
        }

        // 使用LinkedList在头部插入，这样最终结果会按层级从高到低排序
        LinkedList<Topic> orderedResult = new LinkedList<>();
        orderedResult.add(currentTopic);

        // 递归获取父主题
        Long parentId = currentTopic.getParentId();
        while (parentId != null) {
            Topic parentTopic = this.getById(parentId);
            if (parentTopic != null && parentTopic.getParentId() != null && parentTopic.getLevel() != null) {
                orderedResult.addFirst(parentTopic); // 在头部插入父主题
                parentId = parentTopic.getParentId();
            } else {
                break;
            }
        }

        return new ArrayList<>(orderedResult);
    }

    @Override
    public List<TopicPathDTO> getBookTopicPaths(Long bookId) {
        // 1. 获取书籍关联的所有主题ID
        LambdaQueryWrapper<BookTopic> bookTopicWrapper = new LambdaQueryWrapper<>();
        bookTopicWrapper.eq(BookTopic::getBookId, bookId)
                .eq(BookTopic::getIsDeleted, 0);
        List<BookTopic> bookTopics = bookTopicMapper.selectList(bookTopicWrapper);
        
        if (bookTopics.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 获取每个主题的完整路径, 
        List<TopicPathDTO> result = new ArrayList<>();
        for (BookTopic bookTopic : bookTopics) {
            List<Topic> topicPath = getTopicWithParents(bookTopic.getTopicId());
            // 过滤掉parentId为null、level大于等于100的主题
            topicPath = topicPath.stream()
                    .filter(topic -> topic.getParentId() != null && topic.getLevel() != null && topic.getLevel() < 100)
                    .collect(Collectors.toList());

            if (!topicPath.isEmpty()) {
                TopicPathDTO pathDTO = new TopicPathDTO();
                pathDTO.setTopicId(bookTopic.getTopicId());
                // 使用 > 连接主题名称
                String path = topicPath.stream()
                        .map(Topic::getName)
                        .collect(Collectors.joining(" > "));
                pathDTO.setPath(path);
                // 主题级别数量
                pathDTO.setLevelSize(topicPath.size() - 1);
                result.add(pathDTO);
            }
        }

        return result;
    }

    @Override
    public List<Topic> getBookTopicTags(Long bookId) {
        return baseMapper.getBookTopicTags(bookId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean parseBookOPACTopics(Long bookId, String topicContent) {
        if (org.apache.commons.lang3.StringUtils.isBlank(topicContent)) {
            logger.info("书籍[{}]的主题词为空，跳过处理", bookId);
            return false;
        }
        
        logger.info("开始解析书籍[{}]的主题词: {}", bookId, topicContent);
        
        // 固定的顶级父节点ID
        Long rootParentId = 1896773131181346817L;

        //前置处理，关联根节点
        BookTopic bookTopic = new BookTopic();
        bookTopic.setBookId(bookId);
        bookTopic.setTopicId(rootParentId);
        bookTopicMapper.insert(bookTopic);  
        
        // 1. 按逗号分割成多组主题词
        String[] topicGroups = topicContent.split(",");
        
        for (String topicGroup : topicGroups) {
            // 跳过空组
            if (org.apache.commons.lang3.StringUtils.isBlank(topicGroup)) {
                continue;
            }
            
            // 2. 按--分割每组中的关键词
            String[] keywords = topicGroup.trim().split("--");
            
            if (keywords.length == 0) {
                continue;
            }
            
            // 从右到左处理关键词（倒序遍历）
            Long currentParentId = rootParentId;
            for (int i = keywords.length - 1; i >= 0; i--) {
                String keyword = keywords[i].trim();
                if (org.apache.commons.lang3.StringUtils.isBlank(keyword)) {
                    continue;
                }
                
                // 当前层级 (最右边的关键词level为1，向左依次增加)
                int level = keywords.length - i;
                
                // 2.1 查询关键词是否存在
                LambdaQueryWrapper<Topic> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Topic::getName, keyword)
                          .eq(Topic::getLevel, level)
                          .eq(Topic::getParentId, currentParentId);
                          
                Topic topic = baseMapper.selectOne(queryWrapper);
                
                // 2.2 不存在则创建
                if (topic == null) {
                    topic = new Topic();
                    topic.setName(keyword);
                    topic.setLevel(level);
                    topic.setParentId(currentParentId);
                    topic.setCreateTime(new Date());
                    topic.setModifiedTime(new Date());
                    baseMapper.insert(topic);
                    logger.info("创建新主题: {}，层级: {}，父ID: {}", keyword, level, currentParentId);
                }
                
                // 更新父ID为当前主题ID，用于下一个关键词
                currentParentId = topic.getId();
                
                // 2.3 每个关键词都需要与书籍关联，不仅仅是最左边的关键词
                // 检查关联是否已存在
                // BookTopic bookTopic = new BookTopic();
                // bookTopic.setBookId(bookId);
                // bookTopic.setTopicId(topic.getId());
                
                // 查询是否已存在关联
                LambdaQueryWrapper<BookTopic> bookTopicQuery = new LambdaQueryWrapper<>();
                bookTopicQuery.eq(BookTopic::getBookId, bookId)
                            .eq(BookTopic::getTopicId, topic.getId());
                Long count = bookTopicMapper.selectCount(bookTopicQuery);
                
                // 不存在则添加关联
                if (count == 0) {
                    bookTopicMapper.insert(bookTopic);
                    logger.info("书籍[{}]关联主题: {}", bookId, topic.getId());
                }
            }
        }
        
        return true;
    }
} 