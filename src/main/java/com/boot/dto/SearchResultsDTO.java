package com.boot.dto;

import lombok.Data;
import java.util.List;

@Data
public class SearchResultsDTO {
    private String query;
    private List<RecallDTO> recalls;
    private List<NoticeDTO> notices;
    private List<BoardDTO> pressReleases; // 보도자료
    private List<FaqDTO> faqs;

    public SearchResultsDTO(String query) {
        this.query = query;
    }

    // 검색 결과가 하나라도 있는지 확인하는 편의 메서드
    public boolean isEmpty() {
        return (recalls == null || recalls.isEmpty()) &&
               (notices == null || notices.isEmpty()) &&
               (pressReleases == null || pressReleases.isEmpty()) &&
               (faqs == null || faqs.isEmpty());
    }
}
