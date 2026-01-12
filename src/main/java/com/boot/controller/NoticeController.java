package com.boot.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import com.boot.dto.Criteria;
import com.boot.dto.NoticeDTO;
import com.boot.dto.PageDTO;
import com.boot.service.NoticeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/notice")
@RequiredArgsConstructor
@Slf4j
public class NoticeController {
	private final NoticeService noticeService; // 기존에 구현된 Service 주입


    @GetMapping("/list")
    public String noticeList(Criteria cri, Model model) {
        log.info("@# User Notice list requested: {}", cri);
        
        // **[Service 호출]**: NoticeService의 목록 조회 및 전체 개수 메서드 재사용
        List<NoticeDTO> list = noticeService.listWithPaging(cri);
        int total = noticeService.getTotalCount();

        model.addAttribute("list", list);
        model.addAttribute("pageMaker", new PageDTO(cri, total));

        // **[View 지정]**: /WEB-INF/views/notice_list_user.jsp 로 연결
        return "notice_list_user";
    }

    // 2. 공지사항 상세 페이지
    // URL: /notice/123
    @GetMapping("/{notice_id}")
    public String noticeDetail(@PathVariable("notice_id") long notice_id, @ModelAttribute("cri") Criteria cri, Model model) {
        log.info("@# User Notice detail requested: {}", notice_id);
        
        // **[Service 호출]**: 상세 정보 조회 (NoticeServiceImpl에서 조회수 증가 로직 포함)
        NoticeDTO notice = noticeService.getNotice(notice_id);
        
        model.addAttribute("notice", notice);
        
        // **[View 지정]**: /WEB-INF/views/notice_detail_user.jsp 로 연결
        return "notice_detail_user";
    }
}