package com.boot.service;

import com.boot.dao.AdminMapper;
import com.boot.dao.MemberDao;
import com.boot.dto.MemberDto;
import com.boot.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {

    private final MemberDao memberDao;
    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // 비밀번호 재설정 토큰은 보안상 만료 시간이 필요하므로 메모리에서 관리 (실제 운영에서는 Redis 등 사용 권장)
    private final ConcurrentHashMap<String, String> passwordResetTokens = new ConcurrentHashMap<>();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. admin_account 테이블에서 관리자 계정 조회
        UserDto adminUser = adminMapper.findByAdminId(username);
        if (adminUser != null) {
            adminUser.setRole("ROLE_ADMIN");
            return adminUser;
        }

        // 2. member 테이블에서 일반 사용자 계정 조회
        MemberDto member = memberDao.findByUsername(username);
        if (member == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        // 이메일 인증 여부에 따라 계정 활성화 상태 결정
        boolean enabled = member.isEmailVerified();

        return new User(
            member.getUsername(),
            member.getPassword(),
            enabled,
            true,
            true,
            true,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Transactional
    public void save(MemberDto memberDto) {
        if (memberDao.findByUsername(memberDto.getUsername()) != null) {
            throw new IllegalArgumentException("Username already exists.");
        }
        if (memberDao.findByEmail(memberDto.getEmail()) != null) {
            throw new IllegalArgumentException("Email already registered.");
        }

        String verificationToken = UUID.randomUUID().toString();
        memberDto.setPassword(passwordEncoder.encode(memberDto.getPassword()));
        memberDto.setEmailVerified(false);
        memberDto.setEmailVerificationToken(verificationToken);
        
        memberDao.save(memberDto);

        String verificationLink = "http://localhost:8484/verify-email?token=" + verificationToken;
        String emailContent = "안녕하세요, " + memberDto.getUsername() + "님!<br>"
                            + "회원가입을 완료하시려면 다음 링크를 클릭하여 이메일 인증을 해주세요:<br>"
                            + "<a href=\"" + verificationLink + "\">이메일 인증하기</a>";
        emailService.sendEmail(memberDto.getEmail(), "자동차 리콜 통합센터 - 이메일 인증", emailContent);
    }

    @Transactional
    public boolean verifyEmail(String token) {
        MemberDto member = memberDao.findByToken(token);
        if (member != null) {
            memberDao.updateEmailVerified(member.getUsername(), true);
            return true;
        }
        return false;
    }

    public String findUsernameByEmail(String email) {
        MemberDto member = memberDao.findByEmail(email);
        if (member != null) {
            String emailContent = "요청하신 아이디는 <b>" + member.getUsername() + "</b> 입니다.";
            emailService.sendEmail(member.getEmail(), "자동차 리콜 통합센터 - 아이디 찾기", emailContent);
            return member.getUsername();
        }
        return null;
    }

    @Transactional
    public boolean requestPasswordReset(String username, String email) {
        MemberDto member = memberDao.findByUsername(username);
        if (member != null && member.getEmail().equals(email)) {
            String resetToken = UUID.randomUUID().toString();
            passwordResetTokens.put(resetToken, username);

            String resetLink = "http://localhost:8484/reset-password-form?token=" + resetToken;
            String emailContent = "안녕하세요, " + username + "님!<br>"
                                + "비밀번호를 재설정하시려면 다음 링크를 클릭해주세요:<br>"
                                + "<a href=\"" + resetLink + "\">비밀번호 재설정하기</a><br>"
                                + "이 링크는 일정 시간 후 만료됩니다.";
            emailService.sendEmail(member.getEmail(), "자동차 리콜 통합센터 - 비밀번호 재설정", emailContent);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        String username = passwordResetTokens.get(token);
        if (username != null) {
            memberDao.updatePassword(username, passwordEncoder.encode(newPassword));
            passwordResetTokens.remove(token);
            return true;
        }
        return false;
    }

    public boolean isUsernameTaken(String username) {
        return memberDao.findByUsername(username) != null;
    }

    public boolean isEmailTaken(String email) {
        return memberDao.findByEmail(email) != null;
    }

    public MemberDto getMemberByUsername(String username) {
        return memberDao.findByUsername(username);
    }
}
