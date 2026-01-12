package com.boot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserVehicleDto {
    private Long id;
    private String username;
    private String maker;
    private String carModel; // modelName -> carModel로 변경
}
