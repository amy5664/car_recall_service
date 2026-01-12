package com.boot.controller;

import com.boot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final NotificationService notificationService;

    @ModelAttribute("unreadNotificationCount")
    public int addUnreadNotificationCount(Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            return notificationService.countUnreadNotifications(username);
        }
        return 0;
    }
}
