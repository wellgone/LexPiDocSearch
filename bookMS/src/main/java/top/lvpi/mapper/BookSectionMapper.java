package top.lvpi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.lvpi.model.entity.BookSection;
import top.lvpi.model.vo.BookSectionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookSectionMapper extends BaseMapper<BookSection> {
    
    BookSectionVO selectSectionWithBookById(@Param("id") Integer id);
    
    List<BookSectionVO> selectSectionWithBookByIsbn(@Param("isbn") String isbn);
    
    List<BookSectionVO> selectSectionWithBookByBookId(@Param("bookId") Long bookId);
    
    void updateIsIndexedById(@Param("bookId") Long bookId, @Param("isIndexed") Integer isIndexed);
} 