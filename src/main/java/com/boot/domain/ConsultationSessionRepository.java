package com.boot.domain;

import com.boot.dto.ConsultationSessionDTO;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * 상담 세션 MongoDB Repository
 */
@Repository
public interface ConsultationSessionRepository extends ReactiveMongoRepository<ConsultationSessionDTO, String> {
    Mono<ConsultationSessionDTO> findBySessionId(String sessionId);
}
