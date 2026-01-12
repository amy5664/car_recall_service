package com.boot.dto;

import lombok.Data;

@Data
public class NoticeDTO {
    private long notice_id;
    private String title;
    private String content;
    private String is_urgent;
    private int views;
    private String created_at;
    private String updated_at;
}