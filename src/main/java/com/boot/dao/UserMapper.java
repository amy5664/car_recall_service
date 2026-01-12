package com.boot.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.boot.dto.UserDto;

@Mapper
public interface UserMapper {
    UserDto findByAdminId(@Param("adminId") String adminId);
}
