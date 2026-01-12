import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "1230"; // 여기에 원하는 비밀번호를 입력하세요
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("인코딩된 비밀번호: " + encodedPassword);
    }
}
