package com.boot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberDto {
    private String username;
    private String password;
    private String email;
    private boolean emailVerified; // 이메일 인증 여부
    private String emailVerificationToken; // 이메일 인증 토큰
}
