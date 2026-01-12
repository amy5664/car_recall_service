package com.boot.dto;

import lombok.Data;

@Data
public class RecallData {
    private String MAKER;
    private String MODEL_NAME;
    private String RECALL_DATE;
    private String RECALL_REASON;
    private int RECALL_COUNT;
    private double FIX_RATE;
}
