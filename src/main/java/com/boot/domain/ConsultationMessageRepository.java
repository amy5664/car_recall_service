package com.boot.domain;

import com.boot.dto.ConsultationMessageDTO;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * 상담 메시지 MongoDB Repository
 */
@Repository
public interface ConsultationMessageRepository extends ReactiveMongoRepository<ConsultationMessageDTO, String> {
    Flux<ConsultationMessageDTO> findBySessionId(String sessionId);
    Flux<ConsultationMessageDTO> findByAgentId(String agentId);
}
