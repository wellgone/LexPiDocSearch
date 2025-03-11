package top.lvpi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import top.lvpi.model.dto.book.BookAddRequest;
import top.lvpi.model.dto.book.BookQueryRequest;
import top.lvpi.model.dto.book.BookUpdateRequest;
import top.lvpi.model.entity.Book;
import top.lvpi.model.vo.BookVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BookService extends IService<Book> {
    /**
     * 创建图书
     */
    int addBook(BookAddRequest bookAddRequest);

    /**
     * 删除图书
     */
    boolean deleteBook(Long id);

    /**
     * 更新图书
     */
    boolean updateBook(BookUpdateRequest bookUpdateRequest);

    /**
     * 根据 id 获取图书
     */
    Book getBookById(Long id);

    /**
     * 获取图书列表（分页）
     */
    IPage<Book> listBooks(BookQueryRequest bookQueryRequest);

    /**
     * 获取图书总数
     */
    long countBooks(BookQueryRequest bookQueryRequest);

    /**
     * 搜索图书
     */
    IPage<Book> searchBooks(String keyword, int pageNum, int pageSize);

    /**
     * 高级搜索
     */
    IPage<Book> advancedSearch(String keyword, String category, String author, 
                                 Integer yearFrom, Integer yearTo, int page, int size);

    /**
     * 模板搜索
     */
    IPage<Book> templateSearch(String field, String value, int page, int size);

    /**
     * 嵌套搜索
     */
    IPage<Book> nestedSearch(String keyword, Integer maxYear, int page, int size);

    /**
     * 检查图书是否已有章节
     */
    boolean hasBookSections(Long bookId);

    /**
     * 从Excel文件导入图书信息
     * @param file Excel文件
     * @return 导入结果信息
     */
    String importBooksFromExcel(MultipartFile file);

    /**
     * 从Excel文件字节数组导入图书信息
     * @param fileContent Excel文件内容
     * @param fileName 文件名
     * @return 导入结果信息
     */
    String importBooksFromExcel(byte[] fileContent, String fileName);

    /**
     * 获取并保存图书OPAC信息
     * @param id 图书ID
     * @return 处理结果信息
     */
    String getAndSaveOpacInfo(Long id);

    /**
     * 批量获取并保存图书OPAC信息
     * @return 处理结果信息
     */
    String batchGetAndSaveOpacInfo();

    /**
     * 获取所有没有封面的图书列表
     * @return 没有封面的图书列表
     */
    List<Book> getBooksWithoutCover();

    /**
     * 获取所有未获取OPAC信息的图书
     * @return 未获取OPAC信息的图书列表
     */
    List<Book> getBooksWithoutOpac();

    /**
     * 获取所有未提取章节的图书列表
     * @return 未提取章节的图书列表
     */
    List<Book> getBooksWithoutSections();

    /**
     * 获取图书列表（分页）- 包含标签信息
     */
    IPage<BookVO> listBooksWithTags(BookQueryRequest bookQueryRequest);

    /**
     * 获取所有非空的图书类目
     */
    List<String> getAllNonEmptyCategories();

    
    /**
     * 将图书数据转换为BookVO分页列表
     * @param bookPage 图书分页数据
     * @return BookVO分页列表
     */
    IPage<BookVO> convertToBookVOPage(IPage<Book> bookPage);
    
    /**
     * 获取所有需要解析OPAC主题标签的图书
     * @return 需要解析主题的图书列表
     */
    List<Book> listBooksForTopicParsing();
    
    /**
     * 解析图书的OPAC主题标签
     * @param bookId 图书ID
     * @return 是否解析成功
     */
    boolean parseBookOPACTopics(Long bookId);
    
    /**
     * 标记图书的主题解析状态
     * @param bookId 图书ID
     * @param success 是否解析成功
     * @return 是否操作成功
     */
    boolean markTopicParseStatus(Long bookId, boolean success);
} 