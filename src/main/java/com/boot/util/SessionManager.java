package com.boot.util;

import com.boot.dto.MessageNotificationDTO;
import com.boot.dto.WebSocketMessageDTO;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 고객 세션과 상담사 세션을 관리하는 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionManager {
    
    private final Gson gson;
    
    // 고객 세션: sessionId -> CustomerSession
    private final Map<String, CustomerSession> customerSessions = new ConcurrentHashMap<>();
    
    // 상담사 WebSocket 세션: agentId -> WebSocketSession
    private final Map<String, WebSocketSession> agentSessions = new ConcurrentHashMap<>();
    
    // 고객 sessionId -> 상담사 agentId 매핑
    private final Map<String, String> customerToAgentMapping = new ConcurrentHashMap<>();
    
    // WebSocketSession id -> 고객 sessionId 역매핑
    private final Map<String, String> wsSessionToCustomerMapping = new ConcurrentHashMap<>();
    
    /**
     * 고객 세션 추가
     */
    public void addCustomerSession(String sessionId, CustomerSession customerSession) {
        customerSessions.put(sessionId, customerSession);
        wsSessionToCustomerMapping.put(customerSession.getWebSocketSession().getId(), sessionId);
    }
    
    /**
     * 고객 세션 제거
     */
    public void removeCustomerSession(String sessionId) {
        CustomerSession session = customerSessions.remove(sessionId);
        if (session != null) {
            wsSessionToCustomerMapping.remove(session.getWebSocketSession().getId());
            String agentId = customerToAgentMapping.remove(sessionId);
            if (agentId != null) {
                log.info("고객과 상담사 연결 해제: customer={}, agent={}", sessionId, agentId);
            }
        }
    }
    
    /**
     * 고객 세션 조회
     */
    public CustomerSession getCustomerSession(String sessionId) {
        return customerSessions.get(sessionId);
    }
    
    /**
     * 상담사 WebSocket 세션 추가
     */
    public void addAgentSession(String agentId, WebSocketSession webSocketSession) {
        agentSessions.put(agentId, webSocketSession);
        log.info("상담사 세션 등록: agentId={}", agentId);
    }
    
    /**
     * 상담사 WebSocket 세션 제거
     */
    public void removeAgentSession(String agentId) {
        agentSessions.remove(agentId);
        log.info("상담사 세션 제거: agentId={}", agentId);
    }
    
    /**
     * 상담사 세션 조회
     */
    public WebSocketSession getAgentSession(String agentId) {
        return agentSessions.get(agentId);
    }
    
    /**
     * WebSocketSession으로 고객 sessionId 찾기
     */
    public String findSessionIdByWebSocketSession(WebSocketSession webSocketSession) {
        return wsSessionToCustomerMapping.get(webSocketSession.getId());
    }
    
    /**
     * 고객과 상담사 연결
     */
    public void connectCustomerToAgent(String customerSessionId, String agentId) {
        customerToAgentMapping.put(customerSessionId, agentId);
        CustomerSession customerSession = customerSessions.get(customerSessionId);
        if (customerSession != null) {
            customerSession.switchToAgent(agentId);
        }
        log.info("고객과 상담사 연결: customer={}, agent={}", customerSessionId, agentId);
    }
    
    /**
     * 고객과 상담사 연결 해제
     */
    public void disconnectCustomerFromAgent(String customerSessionId) {
        String agentId = customerToAgentMapping.remove(customerSessionId);
        CustomerSession customerSession = customerSessions.get(customerSessionId);
        if (customerSession != null) {
            customerSession.switchToGPT();
        }
        if (agentId != null) {
            log.info("고객과 상담사 연결 해제: customer={}, agent={}", customerSessionId, agentId);
        }
    }
    
    /**
     * 상담사에게 메시지 전송
     */
    public void sendMessageToAgent(String agentId, String customerSessionId, String senderType, String message) {
        WebSocketSession agentSession = agentSessions.get(agentId);
        if (agentSession != null && agentSession.isOpen()) {
            try {
                MessageNotificationDTO notification = new MessageNotificationDTO(
                        customerSessionId, senderType, message
                );
                agentSession.sendMessage(new TextMessage(gson.toJson(notification)));
            } catch (IOException e) {
                log.error("상담사 메시지 전송 오류: agentId={}", agentId, e);
            }
        }
    }
    
    /**
     * 모든 상담사에게 메시지 브로드캐스트
     */
    public void broadcastToAllAgents(String customerSessionId, String senderType, String message) {
        MessageNotificationDTO notification = new MessageNotificationDTO(
                customerSessionId, senderType, message
        );
        String json = gson.toJson(notification);
        
        agentSessions.forEach((agentId, agentSession) -> {
            if (agentSession != null && agentSession.isOpen()) {
                try {
                    agentSession.sendMessage(new TextMessage(json));
                    log.debug("상담사에게 브로드캐스트 전송: agentId={}, sessionId={}", agentId, customerSessionId);
                } catch (IOException e) {
                    log.error("상담사 브로드캐스트 오류: agentId={}", agentId, e);
                }
            }
        });
    }
    
    /**
     * 고객에게 메시지 전송 (상담사 -> 고객)
     * 보내는 메시지는 고객측 chat.js에서 처리할 수 있도록 WebSocketMessageDTO 형식(type="MESSAGE")으로 전송합니다.
     */
    public void sendMessageToCustomer(String customerSessionId, String senderType, String message) {
        CustomerSession customerSession = customerSessions.get(customerSessionId);
        if (customerSession != null && customerSession.getWebSocketSession().isOpen()) {
            try {
                WebSocketMessageDTO notification = new WebSocketMessageDTO("MESSAGE", customerSessionId, message, null);
                customerSession.getWebSocketSession().sendMessage(
                        new TextMessage(gson.toJson(notification))
                );
            } catch (IOException e) {
                log.error("고객 메시지 전송 오류: sessionId={}", customerSessionId, e);
            }
        }
    }

    /**
     * 고객에게 제어 이벤트 전송 (예: AGENT_CONNECTED, AGENT_WAITING 등)
     */
    public void sendEventToCustomer(String customerSessionId, String eventType, String message) {
        CustomerSession customerSession = customerSessions.get(customerSessionId);
        if (customerSession != null && customerSession.getWebSocketSession().isOpen()) {
            try {
                WebSocketMessageDTO event = new WebSocketMessageDTO(eventType, customerSessionId, message, null);
                String json = gson.toJson(event);
                log.info("고객에게 이벤트 전송: sessionId={}, eventType={}, message={}, json={}", 
                         customerSessionId, eventType, message, json);
                customerSession.getWebSocketSession().sendMessage(new TextMessage(json));
                log.info("고객에게 이벤트 전송 완료: sessionId={}, eventType={}", customerSessionId, eventType);
            } catch (IOException e) {
                log.error("고객 이벤트 전송 오류: sessionId={}, eventType={}", customerSessionId, eventType, e);
            }
        } else {
            log.warn("고객 세션을 찾을 수 없거나 WebSocket이 닫혀있음: sessionId={}", customerSessionId);
        }
    }
    
    /**
     * 모든 대기 중인 고객 조회
     */
    public Map<String, CustomerSession> getWaitingCustomers() {
        Map<String, CustomerSession> waiting = new ConcurrentHashMap<>();
        customerSessions.forEach((sessionId, session) -> {
            if ("WAITING".equals(session.getStatus())) {
                waiting.put(sessionId, session);
            }
        });
        return waiting;
    }
    
    /**
     * 모든 활성 고객 조회 (WAITING, AGENT_CHAT 상태)
     */
    public Map<String, CustomerSession> getActiveCustomers() {
        Map<String, CustomerSession> active = new ConcurrentHashMap<>();
        customerSessions.forEach((sessionId, session) -> {
            String status = session.getStatus();
            if ("WAITING".equals(status) || "AGENT_CHAT".equals(status)) {
                active.put(sessionId, session);
            }
        });
        return active;
    }
    
    /**
     * 상담사가 담당하는 고객 목록
     */
    public Map<String, CustomerSession> getCustomersForAgent(String agentId) {
        Map<String, CustomerSession> customers = new ConcurrentHashMap<>();
        customerToAgentMapping.forEach((customerSessionId, assignedAgentId) -> {
            if (agentId.equals(assignedAgentId)) {
                CustomerSession session = customerSessions.get(customerSessionId);
                if (session != null) {
                    customers.put(customerSessionId, session);
                }
            }
        });
        return customers;
    }
}
