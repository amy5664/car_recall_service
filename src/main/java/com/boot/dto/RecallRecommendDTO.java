package com.boot.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RecallRecommendDTO {

    private String maker;
    private String modelName;
    private String recallDate;
    private String recallReason;
    private Double similarity;
}