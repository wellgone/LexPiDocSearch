package top.lvpi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.lvpi.model.entity.Note;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
} 