package top.lvpi.repository.es;

import top.lvpi.model.es.BookSectionDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BookSectionRepository extends ElasticsearchRepository<BookSectionDocument, String> {
    
    void deleteByIsbn(String isbn);
    
    void deleteByBookId(String bookId);
} 