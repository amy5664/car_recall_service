package com.boot.dao;

import com.boot.dto.AdminDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminDAO {
    AdminDTO login(@Param("admin_id") String admin_id);
}