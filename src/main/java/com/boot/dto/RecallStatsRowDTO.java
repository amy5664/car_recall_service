package com.boot.dto;

import lombok.Data;

@Data
public class RecallStatsRowDTO {
	private String maker;        // 제조사
	private String modelName;    // 모델명 (제조사 기준일 때는 빈 문자열)
	private String groupName;    // 그래프용 레이블(제조사/모델 공용)
	private String periodLabel;  // 기간 라벨
	private int recallCount;     // 리콜 건수
}
