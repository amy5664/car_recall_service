package com.boot.controller;

import com.boot.dto.AdminDTO;
import com.boot.dto.BoardDTO;
import com.boot.dto.ComplainDTO;
import com.boot.dto.Criteria;
import com.boot.dto.DailyStatsDTO;
import com.boot.dto.DefectReportDTO;
import com.boot.dto.FaqDTO;
import com.boot.dto.MemberDto; // MemberDto로 수정
import com.boot.dto.NoticeDTO;
import com.boot.dto.PageDTO;
import com.boot.service.AdminService;
import com.boot.service.BoardService;
import com.boot.service.ComplainService;
import com.boot.service.DefectReportService;
import com.boot.service.StatsService;
import com.boot.service.FaqService;
import com.boot.service.MailService;
import com.boot.service.MemberService;
import com.boot.service.NotificationService;
import com.boot.service.NoticeService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.List;
import java.util.HashMap;

@Controller
@Slf4j
@RequestMapping("/admin")
public class AdminController {

    // ===============================================
    // Service Injection
    // ===============================================

    @Autowired
    private AdminService service;

    @Autowired
    private StatsService statsService;

    @Autowired
    private DefectReportService defectReportService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private FaqService faqService;

    @Autowired
    private BoardService boardService;

    @Autowired
    private ComplainService complainService;

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private MailService mailService;

    @Autowired
    private MemberService memberService;

    // ===============================================
    // 인증 및 메인 페이지 (Spring Security 적용 가정)
    // ===============================================

    // Spring Security의 기본 로그인 폼
    @GetMapping("/login")
    public String loginForm() {
        log.info("@# Admin login form");
        return "admin/login";
    }

    /*
     * @PostMapping("/login") 메서드는 Spring Security가 자동으로 처리합니다.
     * 따라서 개발자가 명시적으로 구현할 필요가 없습니다. (첫 번째 코드의 test01/1234 하드코딩 로직 삭제)
     */

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        log.info("@# Admin logout (handled by Spring Security)");
        // Spring Security의 LogoutFilter가 이 URL을 처리하도록 설정되어 있어야 합니다.
        // 일반적으로 SecurityConfig에서 .logout().logoutSuccessUrl("/admin/login?logout") 등으로 설정합니다.
        // 여기서는 Security 설정을 따르도록 "redirect:/admin/login?logout"으로 리다이렉트합니다.
        return "redirect:/admin/login?logout";
    }

    @GetMapping("/main")
    public String adminMain(Model model) {
        log.info("@# Admin main page");

        // 최근 7일간 결함 신고 통계 데이터 조회
        List<DailyStatsDTO> dailyStats = statsService.getDailyReportStats();
        model.addAttribute("dailyStats", dailyStats);

        // 최근 7일간 신고 목록 조회
        List<DefectReportDTO> recentReports = statsService.getRecentReports();
        model.addAttribute("recentReports", recentReports);

        return "admin/main";
    }

    @GetMapping("/consultation")
    public String adminConsultation() {
        log.info("@# Admin consultation dashboard");
        return "admin/consultation";
    }

    // ===============================================
    // 결함 신고 (Defect Reports)
    // ===============================================

    // 결함 신고 목록 페이지 (두 번째 코드의 URL을 더 구체적으로 변경)
    @GetMapping({"/defect_reports", "/defect_reports/list"})
    public String adminDefectReportList(@ModelAttribute("cri") Criteria cri, Model model) {
        log.info("@# Get admin defect report list");
        // "admin"을 전달하여 모든 사용자의 신고를 조회하도록 요청
        List<DefectReportDTO> list = defectReportService.getAllReports(cri, "admin");
        int total = defectReportService.getTotalCount(cri, "admin");
        model.addAttribute("list", list);
        model.addAttribute("pageMaker", new PageDTO(cri, total));
        return "admin/defect_report_list"; // admin/defect_report_list.jsp 뷰 반환
    }

    // 결함 신고 상세 페이지 (첫 번째 코드의 RESTful URL 사용)
    @GetMapping("/defect_reports/{id}")
    public String adminDefectReportDetail(@PathVariable("id") Long id, Model model) {
        log.info("@# Get admin defect report detail: {}", id);
        DefectReportDTO report = defectReportService.getReportById(id);
        model.addAttribute("report", report);
        return "admin/defect_report_detail"; // admin/defect_report_detail.jsp 뷰 반환
    }

    // 결함 신고 상태 업데이트 처리 (첫 번째 코드의 로직 사용)
    @PostMapping("/defect_reports/update-status")
    public String adminUpdateDefectReportStatus(@RequestParam Long id, @RequestParam String status, RedirectAttributes rttr) {
        log.info("@# Update defect report status: id={}, status={}", id, status);
        defectReportService.updateReportStatus(id, status);
        rttr.addFlashAttribute("message", "결함 신고 상태가 업데이트되었습니다.");
        return "redirect:/admin/defect_reports/" + id;
    }

    // ===============================================
    // 공지사항 (Notice)
    // ===============================================

    // 공지사항 목록
    @GetMapping("/notice/list")
    public String noticeList(Criteria cri, Model model) {
        log.info("@# noticeList");
        List<NoticeDTO> list = noticeService.listWithPaging(cri);
        int total = noticeService.getTotalCount();

        model.addAttribute("list", list);
        model.addAttribute("pageMaker", new PageDTO(cri, total));
        return "admin/notice_list";
    }

    // 공지사항 상세
    @GetMapping("/notice/detail")
    public String noticeDetail(@RequestParam("notice_id") Long notice_id, @ModelAttribute("cri") Criteria cri, Model model) {
        log.info("@# noticeDetail, notice_id: {}", notice_id);
        NoticeDTO notice = noticeService.getNotice(notice_id);
        model.addAttribute("notice", notice);
        return "admin/notice_detail";
    }

    // 공지사항 작성 폼
    @GetMapping("/notice/write")
    public String noticeWriteForm() {
        log.info("@# noticeWriteForm");
        return "admin/notice_write";
    }

    // 공지사항 작성 처리
    @PostMapping("/notice/write")
    public String noticeWrite(NoticeDTO notice, RedirectAttributes rttr) {
        log.info("@# noticeWrite, notice: {}", notice);
        noticeService.write(notice);
        rttr.addFlashAttribute("result", "write_success");
        return "redirect:/admin/notice/list";
    }

    // 공지사항 수정 폼
    @GetMapping("/notice/modify")
    public String noticeModifyForm(@RequestParam("notice_id") Long notice_id, @ModelAttribute("cri") Criteria cri, Model model) {
        log.info("@# noticeModifyForm, notice_id: {}", notice_id);
        // 조회수 증가 없이 데이터 가져오기 (메서드명에 따라 예상)
        NoticeDTO notice = noticeService.getNoticeWithoutViews(notice_id);
        model.addAttribute("notice", notice);
        return "admin/notice_modify";
    }

    // 공지사항 수정 처리
    @PostMapping("/notice/modify")
    public String noticeModify(NoticeDTO notice, Criteria cri, RedirectAttributes rttr) {
        log.info("@# noticeModify, notice: {}", notice);
        noticeService.modify(notice);
        rttr.addFlashAttribute("result", "modify_success");
        rttr.addAttribute("pageNum", cri.getPageNum());
        rttr.addAttribute("amount", cri.getAmount());
        return "redirect:/admin/notice/list";
    }

    // 공지사항 삭제 처리
    @PostMapping("/notice/delete")
    public String noticeDelete(@RequestParam("notice_id") Long notice_id, Criteria cri, RedirectAttributes rttr) {
        log.info("@# noticeDelete, notice_id: {}", notice_id);
        noticeService.delete(notice_id);
        rttr.addFlashAttribute("result", "delete_success");
        return "redirect:/admin/notice/list";
    }

    // ===============================================
    // FAQ
    // ===============================================

    // FAQ 목록
    @GetMapping("/faq/list")
    public String faqList(Criteria cri, Model model) {
        log.info("@# faq list");
        List<FaqDTO> list = faqService.getFaqList(cri);
        int total = faqService.getTotal();

        model.addAttribute("list", list);
        model.addAttribute("pageMaker", new PageDTO(cri, total));
        return "admin/faq_list";
    }

    // FAQ 상세
    @GetMapping("/faq/detail")
    public String faqDetail(@RequestParam("faq_id") long faq_id, @ModelAttribute("cri") Criteria cri, Model model) {
        log.info("@# faq detail: {}", faq_id);
        FaqDTO faq = faqService.getFaq(faq_id);
        model.addAttribute("faq", faq);
        return "admin/faq_detail";
    }

    // FAQ 작성 폼
    @GetMapping("/faq/write")
    public String faqWriteForm() {
        log.info("@# faq write view");
        return "admin/faq_write";
    }

    // FAQ 작성 처리
    @PostMapping("/faq/write")
    public String faqWrite(FaqDTO faqDTO, RedirectAttributes rttr) {
        log.info("@# faq write process: {}", faqDTO);
        faqService.writeFaq(faqDTO);
        rttr.addFlashAttribute("result", "write_success");
        return "redirect:/admin/faq/list";
    }

    // FAQ 상세 및 수정 폼
    @GetMapping("/faq/modify")
    public String faqModifyForm(@RequestParam("faq_id") long faq_id, Model model) {
        log.info("@# faq detail/modify: {}", faq_id);
        FaqDTO faq = faqService.getFaq(faq_id);
        model.addAttribute("faq", faq);
        return "admin/faq_modify";
    }

    // FAQ 수정 처리
    @PostMapping("/faq/modify")
    public String faqModify(FaqDTO faqDTO, RedirectAttributes rttr) {
        log.info("@# faq modify process: {}", faqDTO);
        faqService.modifyFaq(faqDTO);
        rttr.addFlashAttribute("result", "modify_success");
        return "redirect:/admin/faq/list";
    }

    // FAQ 삭제 처리
    @PostMapping("/faq/delete")
    public String faqDelete(@RequestParam("faq_id") long faq_id, RedirectAttributes rttr) {
        log.info("@# faq delete: {}", faq_id);
        faqService.deleteFaq(faq_id);
        rttr.addFlashAttribute("result", "delete_success");
        return "redirect:/admin/faq/list";
    }

    // ===============================================
    // 보도자료 (Press/Board)
    // ===============================================

    // 보도자료 목록
    @GetMapping("/press/list")
    public String pressList(Criteria cri, Model model) {
        log.info("@# press list");
        List<BoardDTO> list = boardService.listWithPaging(cri);
        int total = boardService.getTotalCount(cri);

        model.addAttribute("list", list);
        model.addAttribute("pageMaker", new PageDTO(cri, total));
        return "admin/press_list";
    }

    // 보도자료 상세
    @GetMapping("/press/detail")
    public String pressDetail(@RequestParam("boardNo") int boardNo, @ModelAttribute("cri") Criteria cri, Model model) {
        log.info("@# press detail, boardNo: {}", boardNo);
        BoardDTO board = boardService.contentView(boardNo);
        model.addAttribute("board", board);
        return "admin/press_detail";
    }

    // 보도자료 작성 폼
    @GetMapping("/press/write")
    public String pressWriteForm() {
        log.info("@# press write view");
        return "admin/press_write";
    }

    // 보도자료 작성 처리
    @PostMapping("/press/write")
    public String pressWrite(BoardDTO boardDTO, RedirectAttributes rttr) {
        log.info("@# press write process: {}", boardDTO);
        // 파일 정보가 포함된 DTO를 서비스로 전달
        boardService.write(boardDTO);
        rttr.addFlashAttribute("result", "write_success");
        return "redirect:/admin/press/list";
    }

    // 보도자료 수정 폼
    @GetMapping("/press/modify")
    public String pressModifyForm(@RequestParam("boardNo") int boardNo, @ModelAttribute("cri") Criteria cri, Model model) {
        log.info("@# press modify view, boardNo: {}", boardNo);
        BoardDTO board = boardService.contentView(boardNo);
        model.addAttribute("board", board);
        return "admin/press_modify";
    }

    // 보도자료 수정 처리
    @PostMapping("/press/modify")
    public String pressModify(BoardDTO boardDTO, Criteria cri, RedirectAttributes rttr) {
        log.info("@# press modify process: {}", boardDTO);
        boardService.modify(boardDTO);
        rttr.addFlashAttribute("result", "modify_success");
        rttr.addAttribute("pageNum", cri.getPageNum());
        rttr.addAttribute("amount", cri.getAmount());
        return "redirect:/admin/press/list";
    }

    // 보도자료 삭제 처리
    @PostMapping("/press/delete")
    public String pressDelete(@RequestParam("boardNo") int boardNo, Criteria cri, RedirectAttributes rttr) {
        log.info("@# press delete: {}", boardNo);
        boardService.delete(boardNo);
        rttr.addFlashAttribute("result", "delete_success");
        return "redirect:/admin/press/list";
    }

    // ===============================================
    // 고객 문의 (Complain)
    // ===============================================

    // 고객 문의 목록
    @GetMapping("/complain/list")
    public String complainList(Model model) {
        log.info("@# admin complain list");
        List<ComplainDTO> list = complainService.complain_list();
        model.addAttribute("list", list);
        return "admin/complain_list";
    }

    // 고객 문의 상세 및 답변 폼
    @GetMapping("/complain/detail")
    public String complainDetail(@RequestParam("report_id") int report_id, Model model) { // report_id 타입을 int로 변경
        log.info("@# complain detail: {}", report_id);
        ComplainDTO complain = complainService.getComplainById(report_id); // getComplainById 사용
        model.addAttribute("complain", complain);
        return "admin/complain_detail";
    }

    // 고객 문의 답변 등록 처리
    @PostMapping("/complain/answer")
    public String complainAnswer(@RequestParam("report_id") int report_id, @RequestParam("answer") String answer, RedirectAttributes rttr) {
        log.info("@# complain answer process: report_id={}, answer={}", report_id, answer);
        
        // 1. 답변 저장
        HashMap<String, String> param = new HashMap<>();
        param.put("report_id", String.valueOf(report_id)); // int를 String으로 변환
        param.put("answer", answer);
        complainService.addAnswer(param);

        // 2. 문의 정보 조회
        ComplainDTO complain = complainService.getComplainById(report_id);
        if (complain != null) {
            String reporterName = complain.getReporter_name();
            String complainTitle = complain.getTitle();

            // 3. 알림 발송
            String notificationMessage = String.format("작성하신 문의 '%s'에 답변이 등록되었습니다.", complainTitle);
            String notificationLink = "/complain_content_view?report_id=" + report_id;
            notificationService.sendNotification(reporterName, notificationMessage, notificationLink);
            log.info("@# Notification sent to {}: {}", reporterName, notificationMessage);

            // 4. 이메일 발송
            MemberDto member = memberService.getMemberByUsername(reporterName);
            if (member != null && member.getEmail() != null && !member.getEmail().isEmpty()) {
                String to = member.getEmail();
                String subject = "[자동차 리콜 통합센터] 문의하신 '" + complainTitle + "'에 답변이 등록되었습니다.";
                String body = String.format("안녕하세요, %s님.\n\n문의하신 '%s'에 대한 답변이 등록되었습니다.\n\n답변 내용:\n%s\n\n자세한 내용은 홈페이지에서 확인해주세요.\n%s/complain_content_view?report_id=%d",
                                            reporterName, complainTitle, answer, "http://localhost:8484", report_id); // TODO: 실제 도메인으로 변경
                mailService.sendMail(to, subject, body);
                log.info("@# Email sent to {}: {}", to, subject);
            } else {
                log.warn("@# Member email not found or empty for reporter: {}", reporterName);
            }
        } else {
            log.warn("@# Complain not found for report_id: {}", report_id);
        }

        rttr.addFlashAttribute("result", "answer_success");
        return "redirect:/admin/complain/detail?report_id=" + report_id;
    }
}
