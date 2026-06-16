package com.adsplatform.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import redis.clients.jedis.JedisPooled;

@Configuration
public class VectorStoreConfig {

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return new TransformersEmbeddingModel();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        JedisPooled jedis = new JedisPooled("localhost", 6379);
        return RedisVectorStore.builder(jedis, embeddingModel)
                .indexName("brand-safety-index")
                .prefix("brand-safety:")
                .initializeSchema(true)
                .vectorAlgorithm(RedisVectorStore.Algorithm.HNSW)
                .build();
    }
}