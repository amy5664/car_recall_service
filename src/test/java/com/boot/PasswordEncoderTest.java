// PasswordEncoderTest.java
package com.boot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
public class PasswordEncoderTest {

    @Test
    public void encodePassword() {
        // SecurityConfig에 등록한 것과 동일한 인코더를 사용합니다.
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // 암호화할 평문 비밀번호
        String plainPassword = "1234";

        // 비밀번호를 암호화합니다.
        String encodedPassword = passwordEncoder.encode(plainPassword);

        // 콘솔에 암호화된 비밀번호를 출력합니다.
        System.out.println("암호화된 비밀번호: " + encodedPassword);
    }
}
