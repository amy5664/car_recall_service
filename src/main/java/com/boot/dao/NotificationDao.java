package com.boot.dao;

import com.boot.dto.NotificationDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NotificationDao {
    void save(NotificationDto notification);
    List<NotificationDto> findByUsername(String username);
    void markAsRead(Long id);
    int countUnreadNotifications(String username);
}