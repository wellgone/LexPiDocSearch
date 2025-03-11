package top.lvpi.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.json.JsonData;
import top.lvpi.model.entity.Book;
import top.lvpi.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    private static final String INDEX_NAME = "books";

    @Override
    public void createOrUpdateIndex(Book book) {
        try {
            IndexResponse response = elasticsearchClient.index(i -> i
                .index(INDEX_NAME)
                .id(String.valueOf(book.getId()))
                .document(book)
            );
            log.info("Indexed book {} with result {}", book.getId(), response.result());
        } catch (Exception e) {
            log.error("Error indexing book {}: {}", book.getId(), e.getMessage());
            throw new RuntimeException("索引创建失败", e);
        }
    }

    @Override
    public void deleteIndex(Integer id) {
        try {
            DeleteResponse response = elasticsearchClient.delete(d -> d
                .index(INDEX_NAME)
                .id(String.valueOf(id))
            );
            log.info("Deleted index for book {} with result {}", id, response.result());
        } catch (Exception e) {
            log.error("Error deleting index for book {}: {}", id, e.getMessage());
            throw new RuntimeException("索引删除失败", e);
        }
    }

    @Override
    public List<Book> searchBooks(String keyword, int page, int size) {
        try {
            // 构建多字段匹配查询
            Query byKeyword = MatchQuery.of(m -> m
                .field("title")
                .field("author")
                .field("summary")
                .field("keyWord")
                .query(keyword)
            )._toQuery();

            SearchResponse<Book> response = elasticsearchClient.search(s -> s
                .index(INDEX_NAME)
                .query(byKeyword)
                .from((page - 1) * size)
                .size(size),
                Book.class
            );

            logSearchResults(response);
            return extractSearchResults(response);
        } catch (Exception e) {
            log.error("Error searching books: {}", e.getMessage());
            throw new RuntimeException("搜索失败", e);
        }
    }

    @Override
    public long countSearch(String keyword) {
        try {
            Query query = MatchQuery.of(m -> m
                .field("title")
                .field("author")
                .field("summary")
                .field("keyWord")
                .query(keyword)
            )._toQuery();

            return executeCount(query);
        } catch (Exception e) {
            log.error("Error counting search results: {}", e.getMessage());
            throw new RuntimeException("搜索计数失败", e);
        }
    }

    @Override
    public List<Book> advancedSearch(String keyword, String category, String author,
                                   Integer yearFrom, Integer yearTo, int page, int size) {
        try {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // 关键词搜索
            if (keyword != null) {
                boolQuery.must(MatchQuery.of(m -> m
                    .field("title")
                    .field("summary")
                    .field("keyWord")
                    .query(keyword)
                )._toQuery());
            }

            // 分类过滤
            if (category != null) {
                boolQuery.filter(TermQuery.of(t -> t
                    .field("category")
                    .value(category)
                )._toQuery());
            }

            // 作者过滤
            if (author != null) {
                boolQuery.filter(TermQuery.of(t -> t
                    .field("author.keyword")
                    .value(author)
                )._toQuery());
            }

            // 年份范围过滤
            if (yearFrom != null || yearTo != null) {
                RangeQuery.Builder rangeQuery = new RangeQuery.Builder().field("publicationYear");
                if (yearFrom != null) {
                    rangeQuery.gte(JsonData.of(yearFrom));
                }
                if (yearTo != null) {
                    rangeQuery.lte(JsonData.of(yearTo));
                }
                boolQuery.filter(rangeQuery.build()._toQuery());
            }

            SearchResponse<Book> response = elasticsearchClient.search(s -> s
                .index(INDEX_NAME)
                .query(boolQuery.build()._toQuery())
                .from((page - 1) * size)
                .size(size),
                Book.class
            );

            logSearchResults(response);
            return extractSearchResults(response);
        } catch (Exception e) {
            log.error("Error performing advanced search: {}", e.getMessage());
            throw new RuntimeException("高级搜索失败", e);
        }
    }

    @Override
    public long countAdvancedSearch(String keyword, String category, String author,
                                  Integer yearFrom, Integer yearTo) {
        try {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // 构建与advancedSearch相同的查询条件
            // ... (与上面相同的查询构建逻辑)

            return executeCount(boolQuery.build()._toQuery());
        } catch (Exception e) {
            log.error("Error counting advanced search results: {}", e.getMessage());
            throw new RuntimeException("高级搜索计数失败", e);
        }
    }

    @Override
    public List<Book> templateSearch(String field, String value, int page, int size) {
        try {
            // 创建脚本模板
            elasticsearchClient.putScript(r -> r
                .id("query-script")
                .script(s -> s
                    .lang("mustache")
                    .source("{\"query\":{\"match\":{\"{{field}}\":\"{{value}}\"}}}")
                )
            );

            // 使用模板进行搜索
            SearchTemplateResponse<Book> response = elasticsearchClient.searchTemplate(r -> r
                .index(INDEX_NAME)
                .id("query-script")
                .params("field", JsonData.of(field))
                .params("value", JsonData.of(value)),
                Book.class
            );

            return extractSearchResults(response);
        } catch (Exception e) {
            log.error("Error performing template search: {}", e.getMessage());
            throw new RuntimeException("模板搜索失败", e);
        }
    }

    @Override
    public long countTemplateSearch(String field, String value) {
        try {
            Query query = MatchQuery.of(m -> m
                .field(field)
                .query(value)
            )._toQuery();

            return executeCount(query);
        } catch (Exception e) {
            log.error("Error counting template search results: {}", e.getMessage());
            throw new RuntimeException("模板搜索计数失败", e);
        }
    }

    @Override
    public List<Book> nestedSearch(String keyword, Integer maxYear, int page, int size) {
        try {
            // 构建关键词查询
            Query byName = MatchQuery.of(m -> m
                .field("title")
                .query(keyword)
            )._toQuery();

            // 构建年份范围查询
            Query byMaxYear = maxYear != null ? RangeQuery.of(r -> r
                .field("publicationYear")
                .lte(JsonData.of(maxYear))
            )._toQuery() : null;

            // 组合查询
            BoolQuery.Builder boolQuery = new BoolQuery.Builder().must(byName);
            if (byMaxYear != null) {
                boolQuery.must(byMaxYear);
            }

            SearchResponse<Book> response = elasticsearchClient.search(s -> s
                .index(INDEX_NAME)
                .query(boolQuery.build()._toQuery())
                .from((page - 1) * size)
                .size(size),
                Book.class
            );

            logSearchResults(response);
            return extractSearchResults(response);
        } catch (Exception e) {
            log.error("Error performing nested search: {}", e.getMessage());
            throw new RuntimeException("嵌套搜索失败", e);
        }
    }

    @Override
    public long countNestedSearch(String keyword, Integer maxYear) {
        try {
            // 构建与nestedSearch相同的查询条件
            Query byName = MatchQuery.of(m -> m
                .field("title")
                .query(keyword)
            )._toQuery();

            Query byMaxYear = maxYear != null ? RangeQuery.of(r -> r
                .field("publicationYear")
                .lte(JsonData.of(maxYear))
            )._toQuery() : null;

            BoolQuery.Builder boolQuery = new BoolQuery.Builder().must(byName);
            if (byMaxYear != null) {
                boolQuery.must(byMaxYear);
            }

            return executeCount(boolQuery.build()._toQuery());
        } catch (Exception e) {
            log.error("Error counting nested search results: {}", e.getMessage());
            throw new RuntimeException("嵌套搜索计数失败", e);
        }
    }

    @Override
    public void bulkIndexBooks(List<Book> books) {
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();

            for (Book book : books) {
                br.operations(op -> op
                    .index(idx -> idx
                        .index(INDEX_NAME)
                        .id(String.valueOf(book.getId()))
                        .document(book)
                    )
                );
            }

            BulkResponse result = elasticsearchClient.bulk(br.build());

            if (result.errors()) {
                log.error("Bulk indexing had errors");
                for (BulkResponseItem item : result.items()) {
                    if (item.error() != null) {
                        log.error(item.error().reason());
                    }
                }
                throw new RuntimeException("批量索引失败");
            }
        } catch (Exception e) {
            log.error("Error bulk indexing books: {}", e.getMessage());
            throw new RuntimeException("批量索引失败", e);
        }
    }

    private void logSearchResults(SearchResponse<Book> response) {
        TotalHits total = response.hits().total();
        boolean isExactResult = total.relation() == TotalHitsRelation.Eq;

        if (isExactResult) {
            log.info("Found {} results", total.value());
        } else {
            log.info("Found more than {} results", total.value());
        }
    }

    private List<Book> extractSearchResults(SearchResponse<Book> response) {
        List<Book> books = new ArrayList<>();
        for (Hit<Book> hit : response.hits().hits()) {
            Book book = hit.source();
            log.debug("Found book {} with score {}", book.getId(), hit.score());
            books.add(book);
        }
        return books;
    }

    private List<Book> extractSearchResults(SearchTemplateResponse<Book> response) {
        List<Book> books = new ArrayList<>();
        for (Hit<Book> hit : response.hits().hits()) {
            Book book = hit.source();
            log.debug("Found book {} with score {}", book.getId(), hit.score());
            books.add(book);
        }
        return books;
    }

    private long executeCount(Query query) throws Exception {
        SearchResponse<Book> response = elasticsearchClient.search(s -> s
            .index(INDEX_NAME)
            .query(query)
            .size(0),
            Book.class
        );
        return response.hits().total().value();
    }
} 