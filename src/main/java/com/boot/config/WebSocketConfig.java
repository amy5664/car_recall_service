package com.boot.config;

import com.boot.handler.ConsultationWebSocketHandler;
import com.boot.handler.AgentWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket 설정
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final ConsultationWebSocketHandler consultationWebSocketHandler;
    private final AgentWebSocketHandler agentWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 고객용 WebSocket 엔드포인트 (인증 불필요)
        registry.addHandler(consultationWebSocketHandler, "/ws/consultation")
                .setAllowedOrigins("*");
        // 상담사용 WebSocket 엔드포인트 (agentId 쿼리 파라미터 필요)
        registry.addHandler(agentWebSocketHandler, "/ws/agent")
            .setAllowedOrigins("*");
    }
}
