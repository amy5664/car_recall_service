package com.boot.dto;

import lombok.Data;

@Data
public class RecallSimilarDTO {
    private RecallDTO recall;
    private double similarity;

    public RecallSimilarDTO(RecallDTO recall, double similarity) {
        this.recall = recall;
        this.similarity = similarity;
    }
}
