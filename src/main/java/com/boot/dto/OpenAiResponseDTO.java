package com.boot.dto;

import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenAiResponseDTO {
    private List<Choice> choices;
 
    @Getter
    public static class Choice {
        private OpenAiMessageDTO message;
    }
}
