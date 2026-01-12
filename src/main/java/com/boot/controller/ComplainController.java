package com.boot.controller;

import com.boot.dto.ComplainDTO;
import com.boot.service.ComplainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
@Slf4j
public class ComplainController {

    @Autowired
    private ComplainService service;

    @RequestMapping("/complain_write")
    public String write(@ModelAttribute ComplainDTO complainDTO, RedirectAttributes rttr, Principal principal) {
        log.info("@# complain_write");
        log.info("@# complainDTO (before setStatus)=> " + complainDTO);

        // 로그인한 사용자의 경우 reporter_name을 Principal에서 가져와 설정
        if (principal != null) {
            complainDTO.setReporter_name(principal.getName());
            log.info("@# Reporter name set from Principal: " + principal.getName());
        }

        complainDTO.setStatus("접수");
        log.info("@# complainDTO (after setStatus)=> " + complainDTO); // status 값 확인

        service.complain_write(complainDTO);

        rttr.addFlashAttribute("write_result", "success");
        return "redirect:complain_list";
    }

    @RequestMapping("/complain_write_view")
    public String write_view(Model model, Principal principal) {
        log.info("@# complain_write_view");
        if (principal != null) {
            model.addAttribute("loggedInUserName", principal.getName());
        }
        return "complain_write_view";
    }

    @RequestMapping("/complain_content_view")
    public String complain_content_view(@RequestParam HashMap<String, String> param, Model model) {
        log.info("@# complain_content_view()");
        log.info("@# param=>" + param);

        ComplainDTO dto = service.contentView(param);
        model.addAttribute("content_view", dto);

        return "complain_content_view";
    }

    @RequestMapping("/complain_list")
    public String complain_list(Model model, Principal principal) {
        log.info("@# complain_list called.");
        List<ComplainDTO> list;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            log.info("@# User is ADMIN. Fetching all complains.");
            list = service.complain_list();
            model.addAttribute("is_admin", true);
        } else if (principal != null) {
            String reporterName = principal.getName();
            log.info("@# User is logged in. Fetching complains for reporter: " + reporterName);
            list = service.getComplainListByReporterName(reporterName);
            model.addAttribute("is_admin", false);
        } else {
            log.info("@# User is anonymous. Displaying empty list.");
            list = new ArrayList<>();
        }

        log.info("@# Complain list size: " + list.size());
        model.addAttribute("list", list);
        return "complain_list";
    }

    @RequestMapping("/complain_content_modify")
    public String complain_content_modify(@RequestParam("report_id") int reportId, Model model) {
        log.info("@# complain_content_modify() called with reportId: " + reportId);

        ComplainDTO dto = service.getComplainById(reportId);
        log.info("@# Retrieved ComplainDTO for modify: " + dto); // DTO 내용 로깅

        model.addAttribute("m_param", dto);

        return "complain_content_modify";
    }

    @RequestMapping("/complain_modify")
    public String complain_modify(@ModelAttribute ComplainDTO complainDTO,
                                  @RequestParam(value = "newUploadFiles", required = false) List<MultipartFile> newUploadFiles,
                                  @RequestParam(value = "existingFileNames", required = false) List<String> existingFileNames,
                                  RedirectAttributes rttr) {
        log.info("@# complain_modify()");
        try {
            service.complain_modify(complainDTO, newUploadFiles, existingFileNames);
            rttr.addFlashAttribute("message", "상담 내용이 성공적으로 수정되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            rttr.addFlashAttribute("errorMessage", "오류가 발생하여 수정에 실패했습니다: " + e.getMessage());
        }
        return "redirect:/complain_content_view?report_id=" + complainDTO.getReport_id();
    }

    @RequestMapping("/complain_delete")
    public String complain_delete(@RequestParam HashMap<String, String> param, RedirectAttributes rttr) {
        log.info("@# complain_delete()");
        log.info("@# report_id=>" + param.get("report_id"));

        service.complain_delete(param);

        return "redirect:complain_list";
    }

    @RequestMapping("/complain_check_view")
    public String complain_check_view() {
        log.info("@# complain_check_view");
        return "complain_check_view";
    }

    @RequestMapping("/complain_check")
    public String complain_check(@RequestParam("report_id") int reportId, @RequestParam("password") String password, Model model) {
        log.info("@# complain_check");
        ComplainDTO complain = service.getComplainById(reportId);
        if (complain != null && complain.getPassword().equals(password)) {
            model.addAttribute("complain", complain);
            return "complain_status_view";
        }
        return "redirect:complain_check_view?error";
    }
}
