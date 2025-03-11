package top.lvpi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lvpi.model.dto.topic.TopicImportDTO;
import top.lvpi.model.dto.topic.TopicPathDTO;
import top.lvpi.model.dto.topic.TopicTreeDTO;
import top.lvpi.model.entity.Topic;
import java.util.List;

public interface TopicService extends IService<Topic> {
    /**
     * 根据书籍ID获取所有相关主题及其层级关系
     * @param bookId 书籍ID
     * @return 主题列表
     */
    List<Topic> getTopicsByBookId(Long bookId);

    /**
     * 根据书籍ID获取所有相关主题的树形结构
     * @param bookId 书籍ID
     * @return 主题树形结构列表
     */
    List<TopicTreeDTO> getTopicTreeByBookId(Long bookId);

    /**
     * 获取主题及其所有父级主题
     * @param topicId 主题ID
     * @return 主题列表，按层级排序（从顶级主题到当前主题）
     */
    List<Topic> getTopicWithParents(Long topicId);

    /**
     * 获取书籍的所有主题路径（格式化）
     * @param bookId 书籍ID
     * @return 格式化的主题路径列表
     */
    List<TopicPathDTO> getBookTopicPaths(Long bookId);

    /**
     * 批量导入主题
     * @param topicImportDTO 主题导入数据
     * @return 导入的根主题ID
     */
    Long importTopics(TopicImportDTO topicImportDTO);

    List<Topic> getBookTopicTags(Long bookId);
        
    /**
     * 解析书籍OPAC主题词并建立关联
     * @param bookId 书籍ID
     * @param topicContent 主题词内容
     * @return 是否处理成功
     */
    boolean parseBookOPACTopics(Long bookId, String topicContent);
} 