package top.lvpi.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
public class ElasticsearchInitializer implements CommandLineRunner {

    private static final String BOOKS_INDEX = "books";
//    private static final String TOPICS_INDEX = "topics";
    private static final int MAX_RETRIES = 30;
    private static final long RETRY_DELAY = 2000; // 2 seconds

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    private final ElasticsearchOperations elasticsearchOperations;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public ElasticsearchInitializer(ElasticsearchOperations elasticsearchOperations, ResourceLoader resourceLoader) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.resourceLoader = resourceLoader;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void run(String... args) {
        initializeIndex(BOOKS_INDEX, "es/books-mapping.json");
//        initializeIndex(TOPICS_INDEX, "es/topics-mapping.json");
    }

    private void initializeIndex(String indexName, String mappingFile) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                // 读取映射文件
                Resource resource = resourceLoader.getResource("classpath:" + mappingFile);
                String mappingJson = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JsonNode expectedMapping = objectMapper.readTree(mappingJson);
                JSONObject expectedMappings = JSONUtil.parseObj(expectedMapping.get("mappings").toString());

                // 检查索引是否存在
                boolean indexExists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(indexName)))
                    .value();

                //如果索引存在，则不进行创建
                if (indexExists) {
                    log.info("Index {} exists and mapping is up to date", indexName);
                    return;
                }

                //TODO 可能存在逻辑错误，待进一步核验
                // if (indexExists) {
                //     // 获取现有索引的映射
                //     GetIndexResponse getIndexResponse = elasticsearchClient.indices().get(
                //         GetIndexRequest.of(g -> g.index(indexName))
                //     );
                //     JSONObject currentMappingJson = JSONUtil.parseObj(getIndexResponse.get(indexName).mappings().toString().replace("TypeMapping: ", ""));
                    
                //     // 比较映射结构
                //     if (expectedMappings.equals(currentMappingJson)) {
                //         log.info("Index {} exists and mapping is up to date", indexName);
                //         return;
                //     }

                //     log.info("Index {} exists but mapping is different, recreating index", indexName);
                    
                //     // 删除现有索引
                //     DeleteIndexResponse deleteResponse = elasticsearchClient.indices()
                //         .delete(DeleteIndexRequest.of(d -> d.index(indexName)));
                    
                //     if (!deleteResponse.acknowledged()) {
                //         throw new RuntimeException("Failed to delete existing index " + indexName);
                //     }
                // }

                // 创建索引
                CreateIndexResponse response = elasticsearchClient.indices()
                    .create(c -> c
                        .index(indexName)
                        .withJson(new java.io.StringReader(mappingJson))
                    );

                if (response.acknowledged()) {
                    log.info("Successfully created index {}", indexName);
                    return;
                } else {
                    log.error("Failed to create index {}", indexName);
                }
            } catch (Exception e) {
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    log.warn("Failed to initialize Elasticsearch index {}, retrying in 2 seconds... (attempt {}/{})", 
                        indexName, retryCount, MAX_RETRIES, e);
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("Failed to initialize Elasticsearch index {} after {} attempts", 
                        indexName, MAX_RETRIES, e);
                    throw new RuntimeException("Failed to initialize Elasticsearch index", e);
                }
            }
        }
    }
} 