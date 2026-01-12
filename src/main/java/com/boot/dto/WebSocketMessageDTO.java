package com.boot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket을 통해 고객이 보내는 메시지
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageDTO {
    private String type;        // "CONNECT", "MESSAGE", "DISCONNECT", "REQUEST_AGENT"
    private String sessionId;   // 고객 UUID
    private String message;     // 메시지 내용
    private String agentId;     // 상담사 ID (필요한 경우)
}
