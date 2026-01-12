package com.boot.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DefectReportDTO {
    private Long id;
    private String username;
    private String reporterName;
    private String contact;
    private String carModel;
    private String vin;
    private String defectDetails;
    private LocalDateTime reportDate;
    private String password;
    private String status;
    private List<DefectImageDTO> images;
}
