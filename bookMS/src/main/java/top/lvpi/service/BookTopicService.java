package top.lvpi.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.lvpi.model.entity.BookTopic;

public interface BookTopicService extends IService<BookTopic> {
    /**
     * 为书籍添加主题标签
     * @param bookId 书籍ID
     * @param topicId 主题ID
     * @return 新增的关联记录ID，如果已存在则返回已存在的记录ID
     */
    Long addBookTopic(Long bookId, Long topicId);

    /**
     * 删除书籍的主题标签
     * @param bookId 书籍ID
     * @param topicId 主题ID
     * @return 是否删除成功
     */
    boolean removeBookTopic(Long bookId, Long topicId);

    /**
     * 删除书籍的所有主题标签
     * @param bookId 书籍ID
     * @return 是否删除成功
     */
    boolean removeAllBookTopics(Long bookId);
} 