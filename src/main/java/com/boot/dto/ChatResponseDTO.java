package com.boot.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatResponseDTO {
	private String answer;
	 
    public ChatResponseDTO(String answer) {
        this.answer = answer;
    }
 
    public static ChatResponseDTO of(String answer) {
        return new ChatResponseDTO(answer);
    }
}
