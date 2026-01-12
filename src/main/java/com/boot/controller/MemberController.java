package com.boot.controller;

import com.boot.dto.MemberDto;
import com.boot.dto.NotificationDto;
import com.boot.dto.UserVehicleDto;
import com.boot.service.MemberService;
import com.boot.service.NotificationService;
import com.boot.service.UserVehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final UserVehicleService userVehicleService;
    private final NotificationService notificationService; // NotificationService 주입

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // @PostMapping("/login") 메서드는 Spring Security가 처리하므로 제거합니다.
    // 이전의 커스텀 로그인 로직은 MemberService의 loadUserByUsername으로 이동했습니다.

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(MemberDto memberDto, RedirectAttributes redirectAttributes) {
        try {
            memberService.save(memberDto);
            redirectAttributes.addFlashAttribute("message", "회원가입이 완료되었습니다. 이메일 인증을 해주세요.");
            return "redirect:/email-sent";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/signup";
        }
    }

    @GetMapping("/email-sent")
    public String emailSent(@RequestParam(required = false) String message, Model model) {
        if (message != null) {
            model.addAttribute("message", message);
        }
        return "emailSent";
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, RedirectAttributes redirectAttributes) {
        if (memberService.verifyEmail(token)) {
            redirectAttributes.addFlashAttribute("message", "이메일 인증이 완료되었습니다. 이제 로그인할 수 있습니다.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않거나 만료된 인증 링크입니다.");
            return "redirect:/login";
        }
    }

    @GetMapping("/find-account")
    public String findAccount() {
        return "findAccount";
    }

    @PostMapping("/find-id")
    public String findId(@RequestParam String email, RedirectAttributes redirectAttributes) {
        String username = memberService.findUsernameByEmail(email);
        if (username != null) {
            redirectAttributes.addFlashAttribute("message", "입력하신 이메일로 아이디 정보를 전송했습니다.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "해당 이메일로 가입된 아이디를 찾을 수 없습니다.");
        }
        return "redirect:/account-result";
    }

    @PostMapping("/reset-password")
    public String requestPasswordReset(@RequestParam String username, @RequestParam String email, RedirectAttributes redirectAttributes) {
        if (memberService.requestPasswordReset(username, email)) {
            redirectAttributes.addFlashAttribute("message", "입력하신 이메일로 비밀번호 재설정 링크를 전송했습니다.");
            return "redirect:/email-sent";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "아이디 또는 이메일 정보가 일치하지 않습니다.");
            return "redirect:/find-account";
        }
    }

    @GetMapping("/reset-password-form")
    public String resetPasswordForm(@RequestParam String token, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("token", token);
        return "resetPasswordForm";
    }

    @PostMapping("/reset-password-confirm")
    public String resetPasswordConfirm(@RequestParam String token, @RequestParam String newPassword, RedirectAttributes redirectAttributes) {
        if (memberService.resetPassword(token, newPassword)) {
            redirectAttributes.addFlashAttribute("message", "비밀번호가 성공적으로 재설정되었습니다. 새 비밀번호로 로그인해주세요.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "유효하지 않거나 만료된 비밀번호 재설정 링크입니다.");
            return "redirect:/login";
        }
    }

    @GetMapping("/account-result")
    public String accountResult(@RequestParam(required = false) String message, Model model) {
        if (message != null) {
            model.addAttribute("message", message);
        }
        return "accountResult";
    }

    // 사용자 차량 관리 페이지
    @GetMapping("/my-vehicles")
    public String myVehicles(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        String username = principal.getName();
        List<UserVehicleDto> userVehicles = userVehicleService.getUserVehicles(username);
        model.addAttribute("userVehicles", userVehicles);
        return "myVehicles";
    }

    // 사용자 차량 추가
    @PostMapping("/my-vehicles/add")
    public String addMyVehicle(Principal principal, UserVehicleDto userVehicleDto, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        String username = principal.getName();
        userVehicleDto.setUsername(username);
        userVehicleService.addUserVehicle(userVehicleDto);
        redirectAttributes.addFlashAttribute("message", "차량이 성공적으로 등록되었습니다.");
        return "redirect:/my-vehicles";
    }

    // 사용자 차량 삭제
    @PostMapping("/my-vehicles/delete")
    public String deleteMyVehicle(Principal principal, @RequestParam Long id, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        userVehicleService.removeUserVehicle(id);
        redirectAttributes.addFlashAttribute("message", "차량이 성공적으로 삭제되었습니다.");
        return "redirect:/my-vehicles";
    }

    // 사용자 알림 목록 페이지
    @GetMapping("/my-notifications")
    public String myNotifications(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        String username = principal.getName();
        log.info("Fetching notifications for user: {}", username);
        List<NotificationDto> notifications = notificationService.getNotificationsByUsername(username);
        log.info("Found {} notifications for user: {}", notifications.size(), username);
        model.addAttribute("notifications", notifications);
        return "myNotifications"; // myNotifications.jsp 뷰 반환
    }

    // 알림 읽음 처리
    @PostMapping("/my-notifications/mark-as-read")
    public String markNotificationAsRead(Principal principal, @RequestParam Long id, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        // 알림 소유권 확인 로직 추가 (선택 사항)
        // NotificationDto notification = notificationService.getNotificationById(id);
        // if (!notification.getUsername().equals(principal.getName())) {
        //     redirectAttributes.addFlashAttribute("errorMessage", "권한이 없습니다.");
        //     return "redirect:/my-notifications";
        // }
        notificationService.markNotificationAsRead(id);
        redirectAttributes.addFlashAttribute("message", "알림을 읽음 처리했습니다.");
        return "redirect:/my-notifications";
    }
}
