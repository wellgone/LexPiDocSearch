package top.lvpi.controller;

import top.lvpi.common.BaseResponse;
import top.lvpi.model.es.BookSectionDocument;
import top.lvpi.service.BookSectionEsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@Tag(name = "书籍章节ES接口", description = "提供书籍章节的Elasticsearch相关操作")
@RestController
@RequestMapping("/es/books")
@RequiredArgsConstructor
public class BookSectionEsController {

    private final BookSectionEsService bookSectionEsService;

    @Operation(summary = "导入指定ID的章节到ES", description = "根据章节ID将单个章节导入到Elasticsearch")
    @PostMapping("/import/id/{id}")
    public BaseResponse<String> importById(
            @Parameter(description = "章节ID") @PathVariable String id) {
        bookSectionEsService.importById(id);
        return BaseResponse.success("章节导入成功");
    }

    @Operation(summary = "导入指定ISBN的所有章节到ES", description = "根据ISBN将书籍的所有章节导入到Elasticsearch")
    @PostMapping("/import/isbn/{isbn}")
    public BaseResponse<String> importByIsbn(
            @Parameter(description = "书籍ISBN") @PathVariable String isbn) {
        bookSectionEsService.importByIsbn(isbn);
        return BaseResponse.success("书籍章节导入成功");
    }

    @Operation(summary = "导入指定书籍ID的所有章节到ES", description = "根据书籍ID将所有章节导入到Elasticsearch")
    @PostMapping("/import/book/{bookId}")
    public BaseResponse<String> importByBookId(
            @Parameter(description = "书籍ID") @PathVariable Long bookId) {
        bookSectionEsService.importByBookId(bookId);
        return BaseResponse.success("书籍章节导入成功");
    }

    @Operation(summary = "保存或更新章节文档", description = "保存或更新单个章节文档到Elasticsearch")
    @PostMapping
    public BaseResponse<BookSectionDocument> save(
            @Parameter(description = "章节文档") @RequestBody BookSectionDocument document) {
        BookSectionDocument savedDoc = bookSectionEsService.saveOrUpdate(document);
        return BaseResponse.success(savedDoc);
    }

    @Operation(summary = "删除章节文档", description = "根据ID删除单个章节文档")
    @DeleteMapping("/{id}")
    public BaseResponse<String> delete(
            @Parameter(description = "章节ID") @PathVariable String id) {
        bookSectionEsService.deleteById(id);
        return BaseResponse.success("章节删除成功");
    }

    @Operation(summary = "删除指定ISBN的所有章节", description = "根据ISBN删除书籍的所有章节文档")
    @DeleteMapping("/isbn/{isbn}")
    public BaseResponse<String> deleteByIsbn(
            @Parameter(description = "书籍ISBN") @PathVariable String isbn) {
        bookSectionEsService.deleteByIsbn(isbn);
        return BaseResponse.success("书籍章节索引删除成功");
    }

    @Operation(summary = "删除指定书籍ID的所有章节", description = "根据书籍ID删除所有章节文档")
    @DeleteMapping("/book/{bookId}")
    public BaseResponse<String> deleteByBookId(
            @Parameter(description = "书籍ID") @PathVariable String bookId) {
        bookSectionEsService.deleteByBookId(bookId);
        return BaseResponse.success("书籍章节索引删除成功");
    }

    @Operation(summary = "搜索章节", description = "支持多条件搜索章节内容，包括关键词、ISBN、书籍ID、页码等，支持分页")
    @GetMapping("/search")
    public BaseResponse<Page<BookSectionDocument>> search(
            @Parameter(description = "搜索关键词，可选") @RequestParam(required = false) String keyword,
            @Parameter(description = "ISBN，可选") @RequestParam(required = false) String isbn,
            @Parameter(description = "书籍ID，可选") @RequestParam(required = false) String bookId,
            @Parameter(description = "书籍名称，可选") @RequestParam(required = false) String bookTitle,
            @Parameter(description = "作者，可选") @RequestParam(required = false) String author,
            @Parameter(description = "出版社，可选") @RequestParam(required = false) String publisher,
            @Parameter(description = "页码，可选") @RequestParam(required = false) Integer pageNum,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页记录数") @RequestParam(defaultValue = "10") int size) {
        Page<BookSectionDocument> result = bookSectionEsService.search(keyword, isbn, bookId, bookTitle,author, publisher, pageNum, PageRequest.of(page, size));
        return BaseResponse.success(result);
    }
} 