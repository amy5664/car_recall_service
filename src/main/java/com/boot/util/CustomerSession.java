package com.boot.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * 고객 세션 정보 (UUID + WebSocket 세션)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSession {
    private String sessionId;           // 고객 UUID
    private WebSocketSession webSocketSession;
    private String status;              // "GPT_CHAT", "AGENT_CHAT", "WAITING"
    private String agentId;             // 담당 상담사 ID
    
    public CustomerSession(String sessionId, WebSocketSession webSocketSession) {
        this.sessionId = sessionId;
        this.webSocketSession = webSocketSession;
        this.status = "GPT_CHAT";
    }
    
    public void switchToAgent(String agentId) {
        this.status = "AGENT_CHAT";
        this.agentId = agentId;
    }
    
    public void switchToGPT() {
        this.status = "GPT_CHAT";
        this.agentId = null;
    }
    
    public void sendMessage(String message) throws IOException {
        if (webSocketSession != null && webSocketSession.isOpen()) {
            webSocketSession.sendMessage(new org.springframework.web.socket.TextMessage(message));
        }
    }
}
