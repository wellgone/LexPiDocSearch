package top.lvpi.repository.es;

import top.lvpi.model.es.DocSectionDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DocSectionRepository extends ElasticsearchRepository<DocSectionDocument, String> {
    
    void deleteByIsbn(String isbn);
    
    void deleteByDocId(String docId);
} 