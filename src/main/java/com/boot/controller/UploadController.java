package com.boot.controller;

import com.boot.dto.BoardAttachDTO;
import com.boot.dto.ComplainAttachDTO;
import com.boot.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnailator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@Slf4j
public class UploadController {

	@Autowired
	private UploadService service;

	public boolean checkImageType(File file) {
		try {
			String contentType = Files.probeContentType(file.toPath());
			if (contentType == null) return false;
			return contentType.startsWith("image");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@PostMapping("/uploadAjaxAction")
	public ResponseEntity<List<BoardAttachDTO>> uploadAjaxPost(MultipartFile[] uploadFile) {
		log.info("@# uploadAjaxAction() - This method might not be used for complain uploads.");
		List<BoardAttachDTO> list = new ArrayList<>();
		// ... (기존 로직 유지 또는 필요에 따라 수정)
		return new ResponseEntity<>(list, HttpStatus.OK);
	}

	public String getFolder() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(new Date());
	}

	@PostMapping("/uploadFolder")
	public ResponseEntity<String> uploadFolder(MultipartFile[] uploadFile) {
		log.info("@# uploadFolder() - This method might not be used for complain uploads.");
		// ... (기존 로직 유지 또는 필요에 따라 수정)
		return new ResponseEntity<>("File Uploaded!", HttpStatus.OK);
	}

	@GetMapping("/getFileList")
	public ResponseEntity<List<BoardAttachDTO>> getFileList(@RequestParam HashMap<String, String> param) {
		log.info("@# getFileList()");
		// ... (기존 로직 유지 또는 필요에 따라 수정)
		return new ResponseEntity<>(new ArrayList<>(), HttpStatus.OK);
	}

	@GetMapping("/display")
	public ResponseEntity<byte[]> getImage(@RequestParam("uuid") String uuid, @RequestParam("type") String type) {
		log.info("@# getImage(), uuid=>" + uuid + ", type=>" + type);
		try {
			String uploadPath = "";
			String fileName = "";
			boolean isImage = false;

			if ("board".equals(type)) {
				BoardAttachDTO attach = service.findByUuid(uuid);
				if (attach == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
				uploadPath = "C:\\temp3\\upload\\" + attach.getUploadPath();
				fileName = attach.getUuid() + "_" + attach.getFileName();
				isImage = attach.isImage();
			} else if ("complain".equals(type)) {
				ComplainAttachDTO attach = service.findComplainAttachByUuid(uuid);
				if (attach == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
				uploadPath = "C:\\upload\\complain"; // ComplainServiceImpl에서 설정한 경로
				fileName = attach.getUuid() + "_" + attach.getFileName();
				isImage = attach.isImage();
			} else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			File file;
			if (isImage) { // 이미지가 true일 경우 썸네일 요청
				file = new File(uploadPath, "s_" + fileName);
			} else { // 이미지가 아니거나 썸네일이 아닌 경우 원본 파일 요청
				file = new File(uploadPath, fileName);
			}

			log.info("@# file=>" + file);
			if (!file.exists()) {
				log.warn("@# File not found: " + file.getAbsolutePath());
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", Files.probeContentType(file.toPath()));
			return new ResponseEntity<>(FileCopyUtils.copyToByteArray(file), headers, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Error getting image", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/download")
	public ResponseEntity<Resource> download(@RequestParam("uuid") String uuid, @RequestParam("type") String type) {
		log.info("@# download(), uuid=>" + uuid + ", type=>" + type);
		try {
			String uploadPath = "";
			String fileName = "";

			if ("board".equals(type)) {
				BoardAttachDTO attach = service.findByUuid(uuid);
				if (attach == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
				uploadPath = "C:\\temp3\\upload\\" + attach.getUploadPath();
				fileName = attach.getUuid() + "_" + attach.getFileName();
			} else if ("complain".equals(type)) {
				ComplainAttachDTO attach = service.findComplainAttachByUuid(uuid);
				if (attach == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);
				uploadPath = "C:\\upload\\complain"; // ComplainServiceImpl에서 설정한 경로
				fileName = attach.getUuid() + "_" + attach.getFileName();
			} else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			Resource resource = new FileSystemResource(new File(uploadPath, fileName));
			if (!resource.exists()) {
				log.warn("@# File not found for download: " + new File(uploadPath, fileName).getAbsolutePath());
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			String resourceName = resource.getFilename();
			String resourceOriginalName = resourceName.substring(resourceName.indexOf("_") + 1);
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Disposition", "attachment; filename=" + new String(resourceOriginalName.getBytes(StandardCharsets.UTF_8), "ISO-8859-1"));
			return new ResponseEntity<>(resource, headers, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Error downloading file", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/deleteFile")
	public ResponseEntity<String> deleteFile(String uuid) {
		log.info("@# deleteFile() uuid => " + uuid);
		// ... (기존 로직 유지 또는 필요에 따라 수정)
		return new ResponseEntity<>("success", HttpStatus.OK);
	}
}
