package com.boot.dto;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "chat")
public class ChatDTO2 {
    @Id
    private String id;

    private String sender;   // "customer" 또는 "agent"
    private String msg;
    private LocalDateTime createdAt;
}
