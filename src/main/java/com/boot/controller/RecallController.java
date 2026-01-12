package com.boot.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boot.dto.RecallStatsFilterDTO;
import com.boot.dto.RecallStatsRowDTO;
import com.boot.service.RecallService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RecallController {
	
	@Autowired
	private RecallService recallService;
	
	@RequestMapping("/main")
	public String list(Model model, HttpServletRequest request) {
		
		return "main";
	}
	
	// 리콜 통계 화면
    @GetMapping("/recall/stats")
    public String statsPage() {
        return "centers/recall_stats"; 
    }
    
 // 리콜 통계 데이터 API (AJAX)
    @GetMapping("/recall/stats/data")
    @ResponseBody
    public List<RecallStatsRowDTO> getStats(RecallStatsFilterDTO filter) {
        return recallService.getRecallStats(filter);
    }
}
