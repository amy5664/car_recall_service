package com.boot.dao;

import com.boot.dto.MemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberDao {
    void save(MemberDto member);
    MemberDto findByUsername(String username);
    MemberDto findByEmail(String email); // 이메일로 사용자 찾기
    MemberDto findByToken(String token); // 토큰으로 사용자 찾기
    void updateEmailVerified(@Param("username") String username, @Param("emailVerified") boolean emailVerified); // 이메일 인증 상태 업데이트
    void updatePassword(@Param("username") String username, @Param("password") String password); // 비밀번호 업데이트
}
