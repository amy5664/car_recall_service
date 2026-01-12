package com.boot.service;

import com.boot.dto.AdminDTO;

import java.util.HashMap;

public interface AdminService {
    AdminDTO login(HashMap<String, String> param);
}
