package top.lvpi.service;

import top.lvpi.model.entity.Book;

import java.util.List;

public interface SearchService {
    /**
     * 创建或更新索引
     */
    void createOrUpdateIndex(Book book);

    /**
     * 删除索引
     */
    void deleteIndex(Integer id);

    /**
     * 基础搜索
     */
    List<Book> searchBooks(String keyword, int page, int size);

    /**
     * 基础搜索计数
     */
    long countSearch(String keyword);

    /**
     * 高级搜索
     */
    List<Book> advancedSearch(String keyword, String category, String author, 
                             Integer yearFrom, Integer yearTo, int page, int size);

    /**
     * 高级搜索计数
     */
    long countAdvancedSearch(String keyword, String category, String author, 
                           Integer yearFrom, Integer yearTo);

    /**
     * 模板搜索
     */
    List<Book> templateSearch(String field, String value, int page, int size);

    /**
     * 模板搜索计数
     */
    long countTemplateSearch(String field, String value);

    /**
     * 嵌套搜索
     */
    List<Book> nestedSearch(String keyword, Integer maxYear, int page, int size);

    /**
     * 嵌套搜索计数
     */
    long countNestedSearch(String keyword, Integer maxYear);

    /**
     * 批量索引图书
     */
    void bulkIndexBooks(List<Book> books);
} 