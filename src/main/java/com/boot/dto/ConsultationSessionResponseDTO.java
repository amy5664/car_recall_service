package com.boot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 상담 세션 정보를 반환하는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationSessionResponseDTO {
    private String sessionId;
    private String status;          // "WAITING", "CONNECTED", "CLOSED"
    private String agentName;
}
