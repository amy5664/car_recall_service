package com.boot.controller;

import com.boot.domain.ConsultationMessageRepository;
import com.boot.domain.ConsultationSessionRepository;
import com.boot.dto.ConsultationMessageDTO;
import com.boot.dto.ConsultationSessionDTO;
import com.boot.util.CustomerSession;
import com.boot.util.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 관리자용 상담 대시보드 API 컨트롤러
 * ADMIN 권한만 접근 가능
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/consultation")
@PreAuthorize("hasRole('ADMIN')")
public class AdminConsultationController {
    
    private final ConsultationSessionRepository consultationSessionRepository;
    private final ConsultationMessageRepository consultationMessageRepository;
    private final SessionManager sessionManager;
    
    /**
     * 대기 중인 고객 목록 (활성 상태: WAITING, CONNECTED, CHATTING)
     */
    @GetMapping("/waiting-customers")
    public ResponseEntity<Map<String, Object>> getWaitingCustomers() {
        Map<String, CustomerSession> activeCustomers = sessionManager.getActiveCustomers();
        Map<String, Object> result = new HashMap<>();
        
        List<Map<String, String>> customers = new ArrayList<>();
        activeCustomers.forEach((sessionId, session) -> {
            Map<String, String> customer = new HashMap<>();
            customer.put("sessionId", sessionId);
            customer.put("status", session.getStatus());
            customers.add(customer);
        });
        
        result.put("count", customers.size());
        result.put("customers", customers);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 상담사가 고객 수락 (상담 시작)
     */
    @PostMapping("/accept-customer/{customerSessionId}")
    public Mono<ResponseEntity<String>> acceptCustomer(
            @PathVariable String customerSessionId,
            @RequestParam String agentId,
            @RequestParam String agentName) {
        
        log.info("고객 수락: customerSessionId={}, agentId={}", customerSessionId, agentId);
        
        return consultationSessionRepository.findBySessionId(customerSessionId)
                .flatMap(session -> {
                    session.connectAgent(agentId, agentName);
                    sessionManager.connectCustomerToAgent(customerSessionId, agentId);

                    // 고객에게 상담사 연결 완료 이벤트 전송 (WebSocket)
                    sessionManager.sendEventToCustomer(customerSessionId, "AGENT_CONNECTED", "상담사가 연결되었습니다");

                    return consultationSessionRepository.save(session)
                            .then(Mono.just(ResponseEntity.ok("고객 상담이 연결되었습니다")));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * 상담 메시지 목록 조회
     */
    @GetMapping("/messages/{sessionId}")
    public Mono<ResponseEntity<List<ConsultationMessageDTO>>> getConsultationMessages(
            @PathVariable String sessionId) {
        return consultationMessageRepository.findBySessionId(sessionId)
                .collectList()
                .map(ResponseEntity::ok);
    }
    
    /**
     * 상담사의 모든 고객 목록 조회
     */
    @GetMapping("/my-customers/{agentId}")
    public ResponseEntity<List<Map<String, Object>>> getMyCustomers(
            @PathVariable String agentId) {
        
        Map<String, CustomerSession> customers = sessionManager.getCustomersForAgent(agentId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        customers.forEach((sessionId, session) -> {
            Map<String, Object> customer = new HashMap<>();
            customer.put("sessionId", sessionId);
            customer.put("status", session.getStatus());
            result.add(customer);
        });
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 상담 종료
     */
    @PostMapping("/end-consultation/{sessionId}")
    public Mono<ResponseEntity<String>> endConsultation(
            @PathVariable String sessionId) {
        
        log.info("상담 종료: sessionId={}", sessionId);
        
        return consultationSessionRepository.findBySessionId(sessionId)
                .flatMap(session -> {
                    session.close();
                    
                    log.info("상담 종료: sessionId={}", sessionId);
                    
                    // 고객에게 상담 종료 이벤트 전송
                    sessionManager.sendEventToCustomer(
                            sessionId,
                            "CONSULTATION_ENDED",
                            "상담사가 상담을 종료했습니다."
                    );
                    
                    // 메시지 전송 후 연결 해제
                    sessionManager.disconnectCustomerFromAgent(sessionId);
                    
                    return consultationSessionRepository.save(session)
                            .then(Mono.just(ResponseEntity.ok("상담이 종료되었습니다")));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    
    /**
     * 모든 상담 세션 조회
     */
    @GetMapping("/all-sessions")
    public Mono<ResponseEntity<List<ConsultationSessionDTO>>> getAllSessions() {
        return consultationSessionRepository.findAll()
                .collectList()
                .map(ResponseEntity::ok);
    }
    
    /**
     * 상담사가 고객에게 메시지 전송
     */
    @PostMapping("/send-message")
    public Mono<ResponseEntity<String>> sendMessage(
            @RequestBody ConsultationMessageDTO messageDTO) {
        
        log.info("상담사 메시지 전송: sessionId={}, message={}", messageDTO.getSessionId(), messageDTO.getMessage());
        
        return consultationMessageRepository.save(messageDTO)
                .flatMap(saved -> {
                    // 첫 답장 시 상태를 CHATTING으로 변경
                    return consultationSessionRepository.findBySessionId(messageDTO.getSessionId())
                            .flatMap(session -> {
                                if (!session.isAgentReplied()) {
                                    session.startChatting();
                                    return consultationSessionRepository.save(session);
                                }
                                return Mono.just(session);
                            })
                            .then(Mono.defer(() -> {
                                // 고객에게 메시지 전송 (WebSocket 또는 다른 방식)
                                sessionManager.sendMessageToCustomer(
                                        messageDTO.getSessionId(),
                                        "AGENT",
                                        messageDTO.getMessage()
                                );
                                return Mono.just(ResponseEntity.ok("메시지 전송 완료"));
                            }));
                })
                .onErrorResume(e -> {
                    log.error("메시지 저장 오류", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
}
