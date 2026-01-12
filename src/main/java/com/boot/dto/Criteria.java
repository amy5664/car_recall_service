package com.boot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor // 기본 생성자: Criteria() {}
public class Criteria {

    private int pageNum = 1;  // 기본값 1로 초기화
    private int amount = 10;   // 기본값 10으로 초기화
    private String type = "";  // 기본값 "" (빈 문자열)로 초기화
    private String keyword = ""; // 기본값 "" (빈 문자열)로 초기화

    // 고급 검색 필드 추가
    private String maker;
    private String modelName;
    private String startDate;
    private String endDate;

    // 두 번째 코드에 있던 페이지 번호와 수량만 받는 생성자를 오버로딩
    public Criteria(int pageNum, int amount) {
        this.pageNum = pageNum;
        this.amount = amount;
        this.type = ""; // 명시적으로 기본값 설정
        this.keyword = ""; // 명시적으로 기본값 설정
    }

    // 데이터베이스 쿼리에서 사용할 offset 값을 계산하는 메소드
    public int getOffset() {
        return (this.pageNum - 1) * this.amount;
    }
}
