package com.boot.config;

import com.boot.service.MemberService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final MemberService memberService;
    private final UserDetailsService adminDetailsService;
    private final AuthenticationSuccessHandler customAuthenticationSuccessHandler;

    public SecurityConfig(MemberService memberService,
                          @Qualifier("adminDetailsService") UserDetailsService adminDetailsService,
                          AuthenticationSuccessHandler customAuthenticationSuccessHandler) {
        this.memberService = memberService;
        this.adminDetailsService = adminDetailsService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
    }

    // PasswordEncoder Bean 정의를 AppConfig로 이전했으므로 여기서는 제거합니다.

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .antMatcher("/admin/**")
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().hasRole("ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .usernameParameter("admin_id") 
                .passwordParameter("admin_pw")
                .defaultSuccessUrl("/admin/main", true)
                .failureUrl("/admin/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .userDetailsService(adminDetailsService)
            .csrf().and();
        return http.build();
    }
    
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .antMatcher("/api/admin/**")
            .authorizeHttpRequests(authorize -> authorize
                .antMatchers("/api/admin/consultation/**").hasRole("ADMIN")
                .anyRequest().hasRole("ADMIN")
            )
            .csrf().disable(); // API는 상태를 저장하지 않으므로 CSRF 비활성화 유지 가능
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain userSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .antMatchers("/api/consultation/**").permitAll()
                .antMatchers("/ws/**").permitAll()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(customAuthenticationSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout") // 기본 로그아웃 URL 설정
                .logoutSuccessUrl("/login?logout") // 로그아웃 성공 시 리다이렉트될 URL
                .invalidateHttpSession(true) // HTTP 세션 무효화
                .deleteCookies("JSESSIONID") // JSESSIONID 쿠키 삭제
                .permitAll()
            )
            .userDetailsService(memberService)
            .csrf(); // CSRF 보호 기능 활성화
        return http.build();
    }
}
