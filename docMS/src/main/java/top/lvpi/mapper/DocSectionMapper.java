package top.lvpi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.lvpi.model.entity.DocSection;
import top.lvpi.model.vo.DocSectionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocSectionMapper extends BaseMapper<DocSection> {
    
    DocSectionVO selectSectionWithDocById(@Param("id") Integer id);
    
    List<DocSectionVO> selectSectionWithDocByIsbn(@Param("isbn") String isbn);
    
    List<DocSectionVO> selectSectionWithDocByDocId(@Param("docId") Long docId);
    
    void updateIsIndexedById(@Param("docId") Long docId, @Param("isIndexed") Integer isIndexed);
} 