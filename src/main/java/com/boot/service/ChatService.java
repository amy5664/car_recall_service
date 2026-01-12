package com.boot.service;

import org.springframework.stereotype.Service;

import com.boot.OpenAiClient.OpenAiClient;
import com.boot.dto.OpenAiResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {
	private final OpenAiClient openAiClient;
	 
    /**
     * 클라이언트 질문을 받아 OpenAI 응답 텍스트 반환
     */
    public String getAnswer(String question) {
        try {
            OpenAiResponseDTO openAiResponse = openAiClient.getChatCompletion(question);
            
            if (openAiResponse == null || openAiResponse.getChoices() == null || openAiResponse.getChoices().isEmpty()) {
                throw new RuntimeException("OpenAI 응답이 비어있습니다");
            }
            
            return openAiResponse.getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            throw new RuntimeException("GPT 응답 처리 중 오류: " + e.getMessage(), e);
        }
    }
}
