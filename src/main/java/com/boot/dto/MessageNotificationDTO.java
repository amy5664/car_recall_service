package com.boot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 메시지 알림 DTO (상담사나 고객에게 전송)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageNotificationDTO {
    private String sessionId;       // 고객 sessionId
    private String senderType;      // "CUSTOMER", "AGENT"
    private String message;         // 메시지 내용
}
