package com.boot.dto;

import java.util.Date; // Date import 추가
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComplainDTO {
	private int report_id;
	private String reporter_name;
	private String password;
	private String phone;
	private String title;
	private String complain_type;
	private String carNum ;
	private String is_public;
	private Date complainDate; // String -> Date 변경
	private String content;
	private String answer;
	private String status;

	// 파일 업로드를 위한 필드
	private List<MultipartFile> uploadFiles;

	// 첨부파일 정보를 위한 필드
	private List<ComplainAttachDTO> attachList;
}
