package com.boot.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.boot.service.RecallService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/centers")
@RequiredArgsConstructor
public class CentersController {
	
	@Autowired
	private RecallService recallService;

	// 리콜센터 소개
    @GetMapping("/about")
    public String about() {
        // JSP 파일 경로: /WEB-INF/views/centers/about.jsp
        return "centers/about";
    }
    
    // 주변 리콜센터/정비소 찾기
    @GetMapping("/map")
    public String map(@RequestParam(value = "manufacturer", required = false) String manufacturer,
                      Model model) {

        List<String> makers = recallService.getMakerList();

        model.addAttribute("manufacturers", makers);          // 드롭다운용 목록
        model.addAttribute("selectedManufacturer", manufacturer); // 리콜정보 상세에서 넘어올 때

        // JSP: /WEB-INF/views/centers/center_map.jsp
        return "centers/center_map";
    }
}