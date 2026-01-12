package com.boot.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boot.dto.ChatDTO2;
import com.boot.service.ChatService2;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController2 {

    private final ChatService2 chatService;

    // 메시지 저장 & SSE로 push
    @PostMapping("/support")
    public Mono<ChatDTO2> send(@RequestBody ChatDTO2 dto) {
        return chatService.save(dto);
    }

    // SSE: 새 메시지만 실시간 전송
    @GetMapping(value = "/support/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatDTO2> stream() {
        return chatService.stream();
    }
}
