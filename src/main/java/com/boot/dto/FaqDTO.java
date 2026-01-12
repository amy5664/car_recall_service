package com.boot.dto;

import lombok.Data;

@Data
public class FaqDTO {
    private long faq_id;
    private String category;
    private String question;
    private String answer;
    private String created_at;
}
