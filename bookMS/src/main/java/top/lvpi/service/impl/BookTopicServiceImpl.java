package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.lvpi.mapper.BookTopicMapper;
import top.lvpi.model.entity.BookTopic;
import top.lvpi.service.BookTopicService;
import org.springframework.stereotype.Service;

@Service
public class BookTopicServiceImpl extends ServiceImpl<BookTopicMapper, BookTopic> implements BookTopicService {

    @Override
    public Long addBookTopic(Long bookId, Long topicId) {
        // 检查是否已存在该关联
        LambdaQueryWrapper<BookTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookTopic::getBookId, bookId)
                .eq(BookTopic::getTopicId, topicId)
                .eq(BookTopic::getIsDeleted, 0);
        
        BookTopic existingBookTopic = this.getOne(wrapper);
        if (existingBookTopic != null) {
            return existingBookTopic.getId(); // 已存在则返回已存在的记录ID
        }

        // 创建新的关联
        BookTopic bookTopic = new BookTopic();
        bookTopic.setBookId(bookId);
        bookTopic.setTopicId(topicId);
        
        this.save(bookTopic);
        return bookTopic.getId();
    }

    @Override
    public boolean removeBookTopic(Long bookId, Long topicId) {
        LambdaUpdateWrapper<BookTopic> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BookTopic::getBookId, bookId)
                .eq(BookTopic::getTopicId, topicId)
                .eq(BookTopic::getIsDeleted, 0);
        
        return this.remove(wrapper);
    }

    @Override
    public boolean removeAllBookTopics(Long bookId) {
        LambdaUpdateWrapper<BookTopic> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BookTopic::getBookId, bookId)
                .eq(BookTopic::getIsDeleted, 0);
        
        return this.remove(wrapper);
    }
} 