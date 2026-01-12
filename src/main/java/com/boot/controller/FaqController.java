package com.boot.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.boot.dto.Criteria;
import com.boot.dto.FaqDTO;
import com.boot.dto.PageDTO;
import com.boot.service.FaqService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/faq")
@RequiredArgsConstructor
@Slf4j
public class FaqController {
	private final FaqService faqService; // 기존에 구현된 Service 주입

    // 1. FAQ 목록 페이지
    // URL: /faq/list
    @GetMapping("/list")
    public String faqList(Criteria cri, Model model) {
        log.info("@# User Faq list requested: {}", cri);
        
        // **[Service 호출]**: FaqService의 목록 조회 및 전체 개수 메서드 재사용
        List<FaqDTO> list = faqService.getFaqList(cri);
        int total = faqService.getTotal();

        model.addAttribute("list", list);
        model.addAttribute("pageMaker", new PageDTO(cri, total));

        // **[View 지정]**: 유저용 JSP 파일로 연결
        return "faq_list_user"; 
    }

    // 2. FAQ 상세 페이지 (필요한 경우. FAQ는 주로 목록에서 아코디언으로 처리)
    // URL: /faq/123
    @GetMapping("/{faq_id}")
    public String faqDetail(@PathVariable("faq_id") long faq_id, Model model) {
        log.info("@# User Faq detail requested: {}", faq_id);
        
        // **[Service 호출]**: 상세 정보 조회
        FaqDTO faq = faqService.getFaq(faq_id);
        
        model.addAttribute("faq", faq);
        
        // **[View 지정]**: 유저용 JSP 파일로 연결
        return "faq_detail_user";
    }
}
