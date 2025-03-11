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

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.lvpi.repository.es")
public class ElasticsearchConfig extends ElasticsearchConfiguration {
    
    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
            .connectedTo(elasticsearchUri.replace("http://", ""))
            .withConnectTimeout(5000)
            .withSocketTimeout(60000)
            .build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 从URI中解析主机信息
        String[] parts = elasticsearchUri.replace("http://", "").split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        // 创建认证信息
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials(username, password));

        // 创建RestClient
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
    }
} 