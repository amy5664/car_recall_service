package com.boot.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefectImageDTO {
    private Long id; // imageId 대신 id로 변경
    private Long reportId;
    private String fileName;
}
