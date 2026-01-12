package com.boot.service;

import com.boot.dao.ComplainAttachDAO;
import com.boot.dao.ComplainDAO;
import com.boot.dto.ComplainAttachDTO;
import com.boot.dto.ComplainDTO;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnailator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ComplainServiceImpl implements ComplainService {

    @Autowired
    private ComplainDAO complainDAO;

    @Autowired
    private ComplainAttachDAO complainAttachDAO;

    private final String uploadFolder = "C:\\upload\\complain";

    @Override
    public ArrayList<ComplainDTO> complain_list() {
        return complainDAO.complain_list();
    }

    @Override
    public ArrayList<ComplainDTO> find_modify_content(HashMap<String, String> param) {
        return complainDAO.find_modify_content(param);
    }

    @Transactional
    @Override
    public void complain_write(ComplainDTO complainDTO) {
        log.info("@# ComplainServiceImpl.complain_write() start");
        complainDAO.complain_write(complainDTO);
        log.info("@# complain_write 후 report_id => " + complainDTO.getReport_id());

        if (complainDTO.getUploadFiles() != null && !complainDTO.getUploadFiles().isEmpty()) {
            saveFiles(complainDTO.getUploadFiles(), complainDTO.getReport_id());
        }
        log.info("@# ComplainServiceImpl.complain_write() end");
    }

    @Override
    public ComplainDTO contentView(HashMap<String, String> param) {
        log.info("@# ComplainServiceImpl.contentView() start");
        ComplainDTO dto = complainDAO.contentView(param);
        if (dto != null) {
            List<ComplainAttachDTO> attachList = complainAttachDAO.findByReportId(dto.getReport_id());
            dto.setAttachList(attachList);
            log.info("@# attachList => " + attachList);
        }
        log.info("@# ComplainServiceImpl.contentView() end");
        return dto;
    }

    @Transactional
    @Override
    public void complain_modify(ComplainDTO complainDTO, List<MultipartFile> newUploadFiles, List<String> existingFileNames) {
        // 1. 텍스트 정보 업데이트
        complainDAO.update(complainDTO);

        // 2. 기존 파일 처리 (삭제된 파일 제거)
        List<ComplainAttachDTO> currentFiles = complainAttachDAO.findByReportId(complainDTO.getReport_id());
        if (currentFiles != null) {
            for (ComplainAttachDTO file : currentFiles) {
                if (existingFileNames == null || !existingFileNames.contains(file.getFileName())) {
                    // DB에서 파일 정보 삭제
                    complainAttachDAO.delete(file.getUuid());
                    // 실제 파일 및 썸네일 삭제
                    deleteFile(file.getUploadPath(), file.getUuid(), file.getFileName(), file.isImage());
                }
            }
        }

        // 3. 새로운 파일 저장
        if (newUploadFiles != null && !newUploadFiles.isEmpty()) {
            saveFiles(newUploadFiles, complainDTO.getReport_id());
        }
    }

    @Override
    public void complain_delete(HashMap<String, String> param) {
        log.info("@# ComplainServiceImpl delete()");
        int report_id = Integer.parseInt(param.get("report_id"));

        // 첨부 파일 먼저 삭제
        List<ComplainAttachDTO> attachList = complainAttachDAO.findByReportId(report_id);
        if (attachList != null) {
            for (ComplainAttachDTO attach : attachList) {
                deleteFile(attach.getUploadPath(), attach.getUuid(), attach.getFileName(), attach.isImage());
            }
        }
        complainAttachDAO.deleteAll(report_id);

        // 게시글 삭제
        complainDAO.complain_delete(param);
    }

    @Override
    public void addAnswer(HashMap<String, String> param) {
        complainDAO.updateAnswer(param);
    }

    @Override
    public ComplainDTO getComplainById(int reportId) {
        ComplainDTO dto = complainDAO.getComplainById(reportId);
        if (dto != null) {
            List<ComplainAttachDTO> attachList = complainAttachDAO.findByReportId(dto.getReport_id());
            dto.setAttachList(attachList);
        }
        return dto;
    }

    @Override
    public List<ComplainDTO> getComplainListByReporterName(String reporterName) {
        return complainDAO.getComplainListByReporterName(reporterName);
    }

    // 파일 저장 헬퍼 메서드
    private void saveFiles(List<MultipartFile> files, int reportId) {
        File uploadPath = new File(uploadFolder);
        if (!uploadPath.exists()) {
            uploadPath.mkdirs();
        }

        for (MultipartFile multipartFile : files) {
            if (multipartFile.getSize() == 0) continue;

            ComplainAttachDTO attachDTO = new ComplainAttachDTO();
            String originalFileName = multipartFile.getOriginalFilename();
            String uuid = UUID.randomUUID().toString();
            String uploadFileName = uuid + "_" + originalFileName;

            File saveFile = new File(uploadPath, uploadFileName);

            try {
                multipartFile.transferTo(saveFile);

                attachDTO.setUuid(uuid);
                attachDTO.setUploadPath(uploadFolder);
                attachDTO.setFileName(originalFileName);
                attachDTO.setReport_id(reportId);

                if (multipartFile.getContentType().startsWith("image")) {
                    attachDTO.setImage(true);
                    File thumbnailFile = new File(uploadPath, "s_" + uploadFileName);
                    try (FileOutputStream thumbnail = new FileOutputStream(thumbnailFile);
                         FileInputStream fis = new FileInputStream(saveFile)) {
                        Thumbnailator.createThumbnail(fis, thumbnail, 100, 100);
                    }
                }
                complainAttachDAO.insert(attachDTO);
            } catch (IOException e) {
                log.error("File upload error", e);
            }
        }
    }

    // 파일 삭제 헬퍼 메서드
    private void deleteFile(String uploadPath, String uuid, String fileName, boolean isImage) {
        if (fileName == null || uuid == null || uploadPath == null) return;
        try {
            File file = new File(uploadPath, uuid + "_" + fileName);
            Files.deleteIfExists(file.toPath());
            if (isImage) {
                File thumbnail = new File(uploadPath, "s_" + uuid + "_" + fileName);
                Files.deleteIfExists(thumbnail.toPath());
            }
        } catch (IOException e) {
            log.error("File delete error", e);
        }
    }
}
