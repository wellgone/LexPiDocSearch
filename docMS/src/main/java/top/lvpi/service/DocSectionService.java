package top.lvpi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import top.lvpi.model.entity.DocSection;

public interface DocSectionService extends IService<DocSection> {
    
    /**
     * 分页查询图书章节
     *
     * @param current 当前页
     * @param size    每页大小
     * @param docId  图书ID
     * @param content 内容关键字
     * @return 分页结果
     */
    IPage<DocSection> page(Integer current, Integer size, Long docId,Integer pageNum, String title, String content);
    
    /**
     * 更新图书章节并同步更新ES索引
     *
     * @param docSection 图书章节信息
     * @return 是否更新成功
     */
    boolean updateWithEs(DocSection docSection);

    /**
     * 根据secionId删除图书章节并删除对应的ES索引
     *
     * @param docSectionId 图书章节信息
     * @return 是否更新成功
     */
    boolean deleteWithEs(Long docSectionId);
} 