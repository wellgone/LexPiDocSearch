package top.lvpi.service;

import top.lvpi.model.es.DocSectionDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DocSectionEsService {
    
    void importById(String id);
    
    void importByIsbn(String isbn);
    
    void importByDocId(Long docId);
    
    DocSectionDocument saveOrUpdate(DocSectionDocument document);
    
    void deleteById(String id);
    
    void deleteByIsbn(String isbn);
    
    void deleteByDocId(String docId);
    
    /**
     * 搜索章节
     *
     * @param keyword    关键词
     * @param isbn       ISBN
     * @param docId     书籍ID
     * @param author     作者
     * @param publisher  出版社
     * @param pageNum    页码
     * @param pageable   分页参数
     * @return 分页结果
     */
    Page<DocSectionDocument> search(String keyword, String isbn, String docId,String docTitle,
            String author, String publisher, Integer pageNum, Pageable pageable);
    
    /**
     * 导入图书章节到ES
     * @param docId 图书ID
     */
    void importDocSections(Long docId);
} 