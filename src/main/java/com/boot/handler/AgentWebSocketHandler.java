package com.boot.handler;

import com.boot.util.SessionManager;
import com.boot.dto.MessageNotificationDTO;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private final SessionManager sessionManager;
    private final Gson gson;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // agentId는 쿼리 파라미터로 전달됨: /ws/agent?agentId=agent-123
        URI uri = session.getUri();
        String agentId = null;
        if (uri != null && uri.getQuery() != null) {
            String[] parts = uri.getQuery().split("&");
            for (String p : parts) {
                if (p.startsWith("agentId=")) {
                    agentId = p.substring("agentId=".length());
                    break;
                }
            }
        }

        if (agentId == null || agentId.isEmpty()) {
            log.warn("Agent WebSocket 연결 시 agentId 미존재, 세션 종료: wsId={}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // 등록
        session.getAttributes().put("agentId", agentId);
        sessionManager.addAgentSession(agentId, session);
        log.info("상담사 WebSocket 연결됨: agentId={}, wsId={}", agentId, session.getId());

        // 확인 메시지 전송
        MessageNotificationDTO welcome = new MessageNotificationDTO("", "SYSTEM", "상담사 연결이 성공했습니다.");
        try {
            session.sendMessage(new TextMessage(gson.toJson(welcome)));
        } catch (IOException e) {
            log.error("상담사에 확인 메시지 전송 실패: agentId={}", agentId, e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 현재 관리자 클라이언트에서 서버로 보내는 메시지 처리 필요시 확장
        log.debug("Agent WebSocket 수신: wsId={}, payload={}", session.getId(), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Object v = session.getAttributes().get("agentId");
        if (v != null) {
            String agentId = v.toString();
            sessionManager.removeAgentSession(agentId);
            log.info("상담사 WebSocket 연결 해제: agentId={}, wsId={}", agentId, session.getId());
        }
    }
}
