package com.boot.service;

import com.boot.dao.NotificationDao;
import com.boot.dto.MemberDto; // MemberDto로 수정
import com.boot.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationDao notificationDao;
    private final EmailService emailService; // 이메일 발송 로직 제거 후에는 필요 없을 수 있음
    private final MemberService memberService; // 이메일 발송 로직 제거 후에는 필요 없을 수 있음

    @Transactional
    public void createAndSendNotification(String username, String type, String title, String message, String link) {
        NotificationDto notification = new NotificationDto();
        notification.setUsername(username);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationDao.save(notification);

        // 사용자에게 이메일 알림 발송 로직
        try {
            MemberDto memberDto = memberService.getMemberByUsername(username);
            if (memberDto != null && memberDto.getEmail() != null && !memberDto.getEmail().isEmpty()) {
                String emailContent = "<h3>" + title + "</h3>"
                                    + "<p>" + message + "</p>"
                                    + (link != null ? "<p><a href=\"" + link + "\">자세히 보기</a></p>" : "");
                emailService.sendEmail(memberDto.getEmail(), "[자동차 리콜 통합센터] " + title, emailContent);
            } else {
                log.warn("NotificationService: Member email not found or empty for user: {}", username);
            }
        } catch (Exception e) {
            log.error("이메일 발송 중 오류 발생", e);
        }
    }

    // AdminController에서 호출할 메소드 추가
    @Transactional
    public void sendNotification(String username, String message, String link) {
        String type = "ComplainAnswer";
        String title = "문의 답변 등록";
        createAndSendNotification(username, type, title, message, link);
    }

    public List<NotificationDto> getNotificationsByUsername(String username) {
        return notificationDao.findByUsername(username);
    }

    @Transactional
    public void markNotificationAsRead(Long id) {
        notificationDao.markAsRead(id);
    }

    public int countUnreadNotifications(String username) {
        return notificationDao.countUnreadNotifications(username);
    }
}
