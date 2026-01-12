package com.boot.service;

import com.boot.dao.AdminDAO;
import com.boot.dto.AdminDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service("adminDetailsService")
public class AdminDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private AdminDAO adminDAO;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminDTO admin = adminDAO.login(username);

        if (admin == null) {
            throw new UsernameNotFoundException("Admin not found with username: " + username);
        }

        // 관리자에게 "ROLE_ADMIN" 권한 부여
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ADMIN");

        // Spring Security의 User 객체로 변환하여 반환
        return new User(admin.getAdmin_id(), admin.getAdmin_pw(), Collections.singleton(authority));
    }
}