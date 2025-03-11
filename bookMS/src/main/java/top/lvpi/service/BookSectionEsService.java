package top.lvpi.service;

import top.lvpi.model.es.BookSectionDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookSectionEsService {
    
    void importById(String id);
    
    void importByIsbn(String isbn);
    
    void importByBookId(Long bookId);
    
    BookSectionDocument saveOrUpdate(BookSectionDocument document);
    
    void deleteById(String id);
    
    void deleteByIsbn(String isbn);
    
    void deleteByBookId(String bookId);
    
    /**
     * 搜索章节
     *
     * @param keyword    关键词
     * @param isbn       ISBN
     * @param bookId     书籍ID
     * @param author     作者
     * @param publisher  出版社
     * @param pageNum    页码
     * @param pageable   分页参数
     * @return 分页结果
     */
    Page<BookSectionDocument> search(String keyword, String isbn, String bookId,String bookTitle,
            String author, String publisher, Integer pageNum, Pageable pageable);
    
    /**
     * 导入图书章节到ES
     * @param bookId 图书ID
     */
    void importBookSections(Long bookId);
} 