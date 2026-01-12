package com.boot.controller;

import com.boot.domain.ConsultationMessageRepository;
import com.boot.domain.ConsultationSessionRepository;
import com.boot.dto.ConsultationMessageDTO;
import com.boot.dto.ConsultationSessionDTO;
import com.boot.dto.ConsultationSessionResponseDTO;
import com.boot.util.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 고객용 상담 API 컨트롤러
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consultation")
public class ConsultationController {
    
    private final ConsultationSessionRepository consultationSessionRepository;
    private final ConsultationMessageRepository consultationMessageRepository;
    private final SessionManager sessionManager;
    
    /**
     * 상담 세션 정보 조회
     */
    @GetMapping("/session/{sessionId}")
    public Mono<ResponseEntity<ConsultationSessionResponseDTO>> getSession(
            @PathVariable String sessionId) {
        return consultationSessionRepository.findBySessionId(sessionId)
                .map(session -> ResponseEntity.ok(
                        new ConsultationSessionResponseDTO(
                                session.getSessionId(),
                                session.getStatus(),
                                session.getAgentName()
                        )
                ))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * 상담 메시지 목록 조회
     */
    @GetMapping("/messages/{sessionId}")
    public Mono<ResponseEntity<List<ConsultationMessageDTO>>> getMessages(
            @PathVariable String sessionId) {
        return consultationMessageRepository.findBySessionId(sessionId)
                .collectList()
                .map(ResponseEntity::ok);
    }
    
    /**
     * 상담 요청 (상담사 연결)
     */
    @PostMapping("/request-agent/{sessionId}")
    public Mono<ResponseEntity<String>> requestAgent(
            @PathVariable String sessionId) {
        return consultationSessionRepository.findBySessionId(sessionId)
                .flatMap(session -> {
                    session.setStatus("WAITING");
                    return consultationSessionRepository.save(session)
                            .then(Mono.just(ResponseEntity.ok("상담사 연결 대기 중")));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * 상담 종료
     */
    @PostMapping("/end/{sessionId}")
    public Mono<ResponseEntity<String>> endConsultation(
            @PathVariable String sessionId) {
        return consultationSessionRepository.findBySessionId(sessionId)
                .flatMap(session -> {
                    session.close();
                    sessionManager.disconnectCustomerFromAgent(sessionId);
                    return consultationSessionRepository.save(session)
                            .then(Mono.just(ResponseEntity.ok("상담이 종료되었습니다")));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
