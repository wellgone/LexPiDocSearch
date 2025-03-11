package top.lvpi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import top.lvpi.mapper.DocTopicMapper;
import top.lvpi.model.entity.DocTopic;
import top.lvpi.service.DocTopicService;
import org.springframework.stereotype.Service;

@Service
public class DocTopicServiceImpl extends ServiceImpl<DocTopicMapper, DocTopic> implements DocTopicService {

    @Override
    public Long addDocTopic(Long docId, Long topicId) {
        // 检查是否已存在该关联
        LambdaQueryWrapper<DocTopic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocTopic::getDocId, docId)
                .eq(DocTopic::getTopicId, topicId)
                .eq(DocTopic::getIsDeleted, 0);
        
        DocTopic existingDocTopic = this.getOne(wrapper);
        if (existingDocTopic != null) {
            return existingDocTopic.getId(); // 已存在则返回已存在的记录ID
        }

        // 创建新的关联
        DocTopic docTopic = new DocTopic();
        docTopic.setDocId(docId);
        docTopic.setTopicId(topicId);
        
        this.save(docTopic);
        return docTopic.getId();
    }

    @Override
    public boolean removeDocTopic(Long docId, Long topicId) {
        LambdaUpdateWrapper<DocTopic> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DocTopic::getDocId, docId)
                .eq(DocTopic::getTopicId, topicId)
                .eq(DocTopic::getIsDeleted, 0);
        
        return this.remove(wrapper);
    }

    @Override
    public boolean removeAllDocTopics(Long docId) {
        LambdaUpdateWrapper<DocTopic> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DocTopic::getDocId, docId)
                .eq(DocTopic::getIsDeleted, 0);
        
        return this.remove(wrapper);
    }
} 