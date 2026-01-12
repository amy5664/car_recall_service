package com.boot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 상담사와 고객 간의 상담 메시지 저장 DTO
 * MongoDB에 저장됨
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "consultation_messages")
public class ConsultationMessageDTO {
    @Id
    private String id;
    
    private String sessionId;           // 고객 세션 UUID
    private String messageType;         // "CUSTOMER", "AGENT"
    private String message;             // 메시지 내용
    private LocalDateTime timestamp;    // 메시지 시간
    private String agentId;             // 상담사 ID (null이면 미할당)
    
    public ConsultationMessageDTO(String sessionId, String messageType, String message) {
        this.sessionId = sessionId;
        this.messageType = messageType;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
}
