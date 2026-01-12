package com.boot.dao;

import com.boot.dto.UserVehicleDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserVehicleDao {
    void save(UserVehicleDto userVehicle);
    List<UserVehicleDto> findByUsername(String username);
    List<String> findUsernamesByCarModel(@Param("carModel") String carModel); // 특정 차종을 소유한 사용자 아이디 목록 조회
    void delete(Long id);
}
