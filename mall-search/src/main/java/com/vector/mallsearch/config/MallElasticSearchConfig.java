package com.vector.mallsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.Data;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @ClassName MallElasticSearchConfig
 * @Description https://artifacts.elastic.co/javadoc/co/elastic/clients/elasticsearch-java/8.3.3/index.html
 * @Author YuanJie
 * @Date 2022/8/3 12:16
 */
@SpringBootConfiguration
@ConfigurationProperties(prefix = "es")
@Data
public class MallElasticSearchConfig {
    private String host;
    private Integer port;
    public static final RequestOptions COMMON_OPTIONS;

    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
//        builder.addHeader("Authorization", "Bearer " + TOKEN);
//        builder.setHttpAsyncResponseConsumerFactory(
//                new HttpAsyncResponseConsumerFactory
//                        .HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }

    // 同步客户端
    @Bean
    public ElasticsearchClient esClient() {
        // Create the low-level client
        RestClient restClient = RestClient.builder(new HttpHost(this.getHost(), this.getPort(), "http")).build();
        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // And create the API client
        return new ElasticsearchClient(transport);
    }
}
