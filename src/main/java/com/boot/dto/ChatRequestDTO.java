package com.boot.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRequestDTO {
	private String message;

	public ChatRequestDTO(String message) {
		this.message = message;
	}
}
