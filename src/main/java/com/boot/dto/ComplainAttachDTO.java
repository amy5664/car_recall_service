package com.boot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComplainAttachDTO {
    private String uuid;
    private String uploadPath;
    private String fileName;
    private boolean image;
    private int report_id;
}
