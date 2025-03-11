package top.lvpi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lvpi.model.entity.DocTopic;

public interface DocTopicService extends IService<DocTopic> {
    /**
     * 为书籍添加主题标签
     * @param docId 书籍ID
     * @param topicId 主题ID
     * @return 新增的关联记录ID，如果已存在则返回已存在的记录ID
     */
    Long addDocTopic(Long docId, Long topicId);

    /**
     * 删除书籍的主题标签
     * @param docId 书籍ID
     * @param topicId 主题ID
     * @return 是否删除成功
     */
    boolean removeDocTopic(Long docId, Long topicId);

    /**
     * 删除书籍的所有主题标签
     * @param docId 书籍ID
     * @return 是否删除成功
     */
    boolean removeAllDocTopics(Long docId);
} 