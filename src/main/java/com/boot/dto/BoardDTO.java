package com.boot.dto;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BoardDTO {
	private int boardNo;
	private String boardName;
	private String boardTitle;
	private String boardContent;
	private Timestamp boardDate;
	private String boardDate2;
	private int boardHit;
	private int hit;
	
	// 첨부파일 정보
	private List<BoardAttachDTO> attachList;
	
	// 글 작성/수정 시 파일 업로드
	private List<MultipartFile> uploadFile;
}
