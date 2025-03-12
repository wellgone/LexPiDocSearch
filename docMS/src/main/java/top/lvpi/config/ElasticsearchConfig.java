package top.lvpi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.http.HttpHeaders;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@EnableElasticsearchRepositories(basePackages = "top.lvpi.repository.es")
public class ElasticsearchConfig extends ElasticsearchConfiguration {
    
    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        // 显式使用HTTP方案 - 确保使用的是http而非https
        return ClientConfiguration.builder()
            .connectedTo(getHostAndPort(elasticsearchUri))
            .withBasicAuth(username, password)
            .withConnectTimeout(5000)
            .withSocketTimeout(60000)
            .build();
    }
    
    // 安全地解析主机和端口
    private String getHostAndPort(String uri) {
        try {
            URI esUri = new URI(uri);
            return esUri.getHost() + ":" + esUri.getPort();
        } catch (URISyntaxException e) {
            // 降级处理：移除http://并返回
            return uri.replace("http://", "").replace("https://", "");
        }
    }
    
    // 如果上述配置无效，可以尝试完全手动配置客户端
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            // 使用URI类安全解析
            URI esUri = new URI(elasticsearchUri);
            String host = esUri.getHost();
            int port = esUri.getPort();
            
            // 创建认证信息
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
    
            // 创建RestClient - 明确指定http协议
            RestClient restClient = RestClient.builder(new HttpHost(host, port, "http"))
                .setHttpClientConfigCallback(httpClientBuilder -> 
                    httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider)
                        .setMaxConnTotal(100)
                        .setMaxConnPerRoute(20))
                .build();
    
            // 创建传输层
            RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());
    
            // 创建API客户端
            return new ElasticsearchClient(transport);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid Elasticsearch URI: " + elasticsearchUri, e);
        }
    }
} 