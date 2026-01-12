package com.boot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RecallPredictionDTO {

    // Flask에서 보내주는 변수값 매칭
    @JsonProperty("recall_probability")
    //정확도
    private Double recallProbability;

    //예상 부품
    @JsonProperty("predicted_part")
    private String predictedPart;

    //유사 사례
    @JsonProperty("similar_case")
    private SimilarCaseDetailDTO similarCase;

    //similar_case 안에 들어있는 데이터를 받을 내부 클래스.
    @Data
    @NoArgsConstructor
    public static class SimilarCaseDetailDTO {
        private String reason;      // 리콜 사유
        private String category;    // 부품 카테고리
        private Double similarity;  // 유사도 점수
    }

}