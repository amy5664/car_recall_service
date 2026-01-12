package com.boot.domain;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import com.boot.dto.ChatDTO2;

public interface ChatRepository extends ReactiveMongoRepository<ChatDTO2, String> {
    // 아무 메서드도 넣지 말 것!
}


