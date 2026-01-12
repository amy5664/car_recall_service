package com.boot.dto;

import lombok.Data;

@Data
public class RecallStatsFilterDTO {
	private String groupBy;
	private String timeUnit;
	private String startDate;
	private String endDate;
	private String maker;
	private String modelKeyword;
}
