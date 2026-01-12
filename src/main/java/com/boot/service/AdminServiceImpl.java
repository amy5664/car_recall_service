package com.boot.service;

import com.boot.dao.AdminDAO;
import com.boot.dto.AdminDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminDAO adminDAO;

    @Override
    public AdminDTO login(HashMap<String, String> param) {
        log.info("@# login");
        log.info("@# param: {}", param);

        // @Mapper 어노테이션으로 등록된 AdminDAO Bean을 직접 사용
        AdminDTO admin = adminDAO.login(param.get("admin_id"));

        // DB에서 가져온 admin 객체 정보를 로그로 출력
        log.info("@# adminDAO.login() result: {}", admin);

        if (admin != null && admin.getAdmin_pw().equals(param.get("admin_pw"))) {
            return admin;
        }
        return null;
    }
}
