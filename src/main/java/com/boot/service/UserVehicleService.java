package com.boot.service;

import com.boot.dao.UserVehicleDao;
import com.boot.dto.UserVehicleDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserVehicleService {

    private final UserVehicleDao userVehicleDao;

    @Transactional
    public void addUserVehicle(UserVehicleDto userVehicleDto) {
        userVehicleDao.save(userVehicleDto);
    }

    public List<UserVehicleDto> getUserVehicles(String username) {
        return userVehicleDao.findByUsername(username);
    }

    public List<String> getUsernamesByCarModel(String carModel) {
        return userVehicleDao.findUsernamesByCarModel(carModel);
    }

    @Transactional
    public void removeUserVehicle(Long id) {
        userVehicleDao.delete(id);
    }
}
