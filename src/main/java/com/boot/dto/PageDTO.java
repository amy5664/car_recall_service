package com.boot.dto;

import lombok.Data;
// @Data 어노테이션은 @Getter, @Setter, @ToString 등을 모두 포함합니다.

@Data
public class PageDTO {

    private int startPage;  // 화면에 보여지는 페이지 시작 번호
    private int endPage;    // 화면에 보여지는 페이지 끝 번호
    private boolean prev, next; // 이전, 다음 버튼 표시 여부

    private int total;      // 전체 데이터 수
    private Criteria cri;   // 현재 페이지, 페이지당 데이터 수

    public PageDTO(Criteria cri, int total) {
        this.cri = cri;
        this.total = total;

        // 1. 화면에 보여질 마지막 페이지 번호 (endPage) 계산
        // 10.0으로 나누어 계산하여 페이지 블록 크기를 10개로 설정합니다. (첫 번째 코드 유지)
        this.endPage = (int) (Math.ceil(cri.getPageNum() / 10.0)) * 10;

        // 2. 화면에 보여질 시작 페이지 번호 (startPage) 계산
        this.startPage = this.endPage - 9; // endPage가 10일 때 startPage가 1이 되도록 설정

        // 3. 전체 마지막 페이지 번호 (realEnd) 계산
        int realEnd = (int) (Math.ceil((total * 1.0) / cri.getAmount()));

        // 4. endPage가 realEnd보다 크면 realEnd로 변경
        if (realEnd < this.endPage) {
            this.endPage = realEnd;
        }

        // 5. 이전(prev) 버튼 표시 여부
        this.prev = this.startPage > 1;

        // 6. 다음(next) 버튼 표시 여부
        this.next = this.endPage < realEnd;

        // 7. 디버그 출력 코드는 실제 운영 코드에서는 불필요하므로 제거합니다.
        // 필요하다면, 로깅 라이브러리(Slf4j 등)를 사용하여 Log로 출력하는 것이 좋습니다.
        /*
        System.out.println("=== PageDTO Debug ===");
        System.out.println("pageNum: " + cri.getPageNum());
        System.out.println("amount: " + cri.getAmount());
        System.out.println("total: " + total);
        System.out.println("startPage: " + startPage);
        System.out.println("endPage: " + endPage);
        System.out.println("realEnd: " + realEnd);
        System.out.println("=====================");
        */
    }
}