package com.boot.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenAiMessageDTO {
    private String role;
 
    private String content;
 
    public OpenAiMessageDTO(String role, String content) {
        this.role = role;
        this.content = content;
    }

}
