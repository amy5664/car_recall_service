package com.boot.config;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import com.boot.dto.ChatDTO2;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
public class MongoConfig {

    private final ReactiveMongoTemplate mongoTemplate;

    @PostConstruct
    public void init() {
        mongoTemplate.collectionExists("chat")
            .flatMap(exists -> {
                if (!exists) {
                    return mongoTemplate.createCollection(
                            "chat",
                            CollectionOptions.empty()
                                    .capped()
                                    .size(10 * 1024 * 1024)
                                    .maxDocuments(10000)
                    );
                }
                return mongoTemplate.findAll(ChatDTO2.class).then();
            })
            .subscribe();
    }
}
