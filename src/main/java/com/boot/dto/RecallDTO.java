package com.boot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecallDTO {
    private Long id;
    private String maker;
    private String modelName;
    private String makeStart;
    private String makeEnd;
    private String recallDate;
    private String recallReason;
    private String vin; // 차대번호 추가
    private String registrationNumber; // 자동차 등록번호 추가
}
