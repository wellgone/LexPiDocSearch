package top.lvpi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.lvpi.model.entity.Topic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TopicMapper extends BaseMapper<Topic> {
    List<Topic> getDocTopicTags(@Param("docId") Long docId);
} 