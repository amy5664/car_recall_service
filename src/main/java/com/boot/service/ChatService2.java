package com.boot.service;

import org.springframework.stereotype.Service;

import com.boot.domain.ChatRepository;
import com.boot.dto.ChatDTO2;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
@RequiredArgsConstructor
public class ChatService2 {

    private final ChatRepository chatRepository;

    private final Sinks.Many<ChatDTO2> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public Mono<ChatDTO2> save(ChatDTO2 dto) {
        sink.tryEmitNext(dto);     // SSE로 바로 발송
        return chatRepository.save(dto);  // DB 저장
    }

    public Flux<ChatDTO2> stream() {
        return sink.asFlux();       // 새 메시지만 전달
    }
}
