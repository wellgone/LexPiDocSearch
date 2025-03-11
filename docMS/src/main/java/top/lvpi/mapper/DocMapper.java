package top.lvpi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.lvpi.model.entity.Doc;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
public interface DocMapper extends BaseMapper<Doc> {

        /**
     * 按分类统计书籍数量
     *
     * @return 包含分类名称和对应数量的列表
     */
    @Select("SELECT category as name, COUNT(*) as value FROM lp_doc WHERE is_deleted = 0 AND category IS NOT NULL AND category != '' GROUP BY category ORDER BY value DESC LIMIT 10")
    List<Map<String, Object>> selectDocCountByCategory();

    /**
     * 按丛书名统计书籍数量
     *
     * @return 包含丛书名称和对应数量的列表
     */
    @Select("SELECT opac_series as name, COUNT(*) as value FROM lp_doc WHERE is_deleted = 0 AND opac_series IS NOT NULL AND opac_series != '' GROUP BY opac_series ORDER BY value DESC LIMIT 50")
    List<Map<String, Object>> selectDocCountBySeries();
} 