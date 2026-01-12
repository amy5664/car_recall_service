package com.boot.service;

import com.boot.dao.ComplainAttachDAO;
import com.boot.dao.UploadDAO;
import com.boot.dto.BoardAttachDTO;
import com.boot.dto.ComplainAttachDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class UploadServiceImpl implements UploadService{

	@Autowired
	private SqlSession sqlSession;

	@Autowired
	private ComplainAttachDAO complainAttachDAO;

	@Override
	public void insertFile(BoardAttachDTO vo) {
		log.info("@# insertFile()");
		log.info("@# vo=>"+vo);
		
		UploadDAO dao = sqlSession.getMapper(UploadDAO.class);
		dao.insertFile(vo);
	}

	@Override
	public List<BoardAttachDTO> getFileList(int boardNo) {
		log.info("@# getFileList()");
		log.info("@# boardNo=>"+boardNo);
		
		UploadDAO dao = sqlSession.getMapper(UploadDAO.class);
		
		return dao.getFileList(boardNo);
	}

	@Override
	public void deleteFile(List<BoardAttachDTO> fileList) {
		log.info("@# deleteFile()");
		log.info("@# fileList=>"+fileList);
		
		if (fileList == null || fileList.size() == 0) {
			return;
		}
		
		fileList.forEach(attach -> {
			try {
				Path file = Paths.get("C:\\temp3\\upload\\"+attach.getUploadPath()+"\\"
														 +attach.getUuid()+"_"
														 +attach.getFileName()
									 );
				Files.deleteIfExists(file);
		
				String uploadFolder = "C:\\temp3\\upload";
				String uploadFolderPath = getFolder();
				File uploadPath = new File(uploadFolder, uploadFolderPath);
				String uploadFileName = attach.getUuid()+"_"+attach.getFileName();
				File saveFile = new File(uploadPath, uploadFileName);
				
				String contentType = Files.probeContentType(saveFile.toPath());
				log.info("@# contentType=>"+contentType);

				if (contentType == null) {
					return;
				}
				
				if (Files.probeContentType(file).startsWith("image")) {
					Path thumbNail = Paths.get("C:\\temp3\\upload\\"+attach.getUploadPath()+"\\s_"
															 +attach.getUuid()+"_"
															 +attach.getFileName()
										 );
					Files.delete(thumbNail);
				}
			} catch (Exception e) {
				log.error("delete file error=>"+e.getMessage());
			}
		});
	}
	
	public String getFolder() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date date=new Date();
		String str = sdf.format(date);
		log.info("@# str=>"+str);
		
		return str;
	}

	@Override
	public BoardAttachDTO findByUuid(String uuid) {
		UploadDAO dao = sqlSession.getMapper(UploadDAO.class);
		return dao.findByUuid(uuid);
	}

	@Override
	public void deleteFileDB(String uuid) {
		UploadDAO dao = sqlSession.getMapper(UploadDAO.class);
		dao.deleteFileDB(uuid);
	}

	@Override
	public ComplainAttachDTO findComplainAttachByUuid(String uuid) {
		return complainAttachDAO.findByUuid(uuid);
	}
}
