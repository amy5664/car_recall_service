package com.boot.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.boot.dto.UserDto; // AuthUserDto로 변경하는 것을 권장합니다.

@Mapper
public interface AdminMapper {
    UserDto findByAdminId(@Param("adminId") String adminId);
}
