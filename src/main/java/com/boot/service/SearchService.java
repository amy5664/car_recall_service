package com.boot.service;

import com.boot.dto.SearchResultsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final RecallService recallService;
    private final NoticeService noticeService;
    private final BoardService boardService;
    private final FaqService faqService;

    public SearchResultsDTO searchAll(String query, String category) {
        log.info("Performing search for query: '{}', category: '{}'", query, category);
        SearchResultsDTO results = new SearchResultsDTO(query);

        // 각 서비스의 검색 메서드 호출 (카테고리에 따라 필터링)
        if ("all".equals(category) || "recall".equals(category)) {
            results.setRecalls(recallService.searchRecallsByModelName(query));
            log.info("Recalls found: {}", results.getRecalls().size());
        }
        if ("all".equals(category) || "notice".equals(category)) {
            results.setNotices(noticeService.searchByKeyword(query));
            log.info("Notices found: {}", results.getNotices().size());
        }
        if ("all".equals(category) || "press".equals(category)) {
            results.setPressReleases(boardService.searchByKeyword(query));
            log.info("Press Releases found: {}", results.getPressReleases().size());
        }
        if ("all".equals(category) || "faq".equals(category)) {
            results.setFaqs(faqService.searchByKeyword(query));
            log.info("FAQs found: {}", results.getFaqs().size());
        }

        return results;
    }
}
