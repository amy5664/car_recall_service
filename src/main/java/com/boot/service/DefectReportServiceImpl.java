package com.boot.service;

import com.boot.dao.DefectImageDAO;
import com.boot.dao.DefectReportDAO;
import com.boot.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefectReportServiceImpl implements DefectReportService {

    private final DefectReportDAO defectReportDAO;
    private final DefectImageDAO defectImageDAO;
    private final NotificationService notificationService; // NotificationService 주입
    private static final String FLASK_API_URL = "http://localhost:5000/predict";
    private static final String FLASK_RECOMMEND_URL = "http://localhost:5000/recommend";

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 단일 파일 저장 로직
    private String saveFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String savedFileName = UUID.randomUUID().toString() + extension;
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        file.transferTo(uploadPath.resolve(savedFileName));
        return savedFileName;
    }

    // 파일 삭제 로직
    private void deleteFile(String fileName) {
        if (fileName != null && !fileName.isEmpty()) {
            Path filePath = Paths.get(uploadDir, fileName);
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                System.err.println("파일 삭제 실패: " + fileName + " - " + e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void saveReport(DefectReportDTO report, List<MultipartFile> files) {
        // 1. 신고 정보 저장 (ID가 생성됨)
        defectReportDAO.insertReport(report);

        // 2. 이미지 파일 저장 및 DB에 이미지 정보 저장
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                try {
                    String savedFileName = saveFile(file);
                    if (savedFileName != null) {
                        DefectImageDTO image = new DefectImageDTO();
                        image.setReportId(report.getId()); // 새로 생성된 신고 ID 사용
                        image.setFileName(savedFileName);
                        defectImageDAO.insertImage(image);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("이미지 파일 저장 중 오류 발생", e);
                }
            }
        }
    }

    @Override
    public List<DefectReportDTO> getAllReports(Criteria cri, String username) { // username 파라미터 추가
        List<DefectReportDTO> reports = defectReportDAO.selectAll(cri, username); // username 전달
        // 각 신고에 이미지 목록을 추가
        for (DefectReportDTO report : reports) {
            report.setImages(defectImageDAO.selectImagesByReportId(report.getId()));
        }
        return reports;
    }

    @Override
    public List<DefectReportDTO> getAllReportsWithoutPaging() {
        // CSV 다운로드용이므로 이미지는 제외하고 신고 데이터만 반환
        return defectReportDAO.selectAllWithoutPaging();
    }

    @Override
    public int getTotalCount(Criteria cri, String username) { // username 파라미터 추가
        return defectReportDAO.count(cri.getKeyword(), username); // username 전달
    }

    @Override
    public DefectReportDTO getReportById(Long id) {
        DefectReportDTO report = defectReportDAO.selectById(id);
        if (report != null) {
            report.setImages(defectImageDAO.selectImagesByReportId(id));
        }
        return report;
    }

    @Override
    @Transactional
    public void updateReport(DefectReportDTO report, List<MultipartFile> newFiles, List<String> existingFileNames) {
        // 1. 신고 정보 업데이트
        defectReportDAO.updateReport(report);

        // 2. 기존 이미지 처리 (삭제된 이미지 파일 및 DB 레코드 제거)
        // 현재 DB에 저장된 이미지 파일 목록을 가져옵니다.
        List<DefectImageDTO> currentImagesInDb = defectImageDAO.selectImagesByReportId(report.getId());
        List<String> currentFileNamesInDb = new ArrayList<>();
        if (currentImagesInDb != null) {
            for (DefectImageDTO img : currentImagesInDb) {
                currentFileNamesInDb.add(img.getFileName());
            }
        }

        // existingFileNames (JSP에서 넘어온 기존 이미지 목록)에 없는 파일은 삭제
        for (String dbFileName : currentFileNamesInDb) {
            if (existingFileNames == null || !existingFileNames.contains(dbFileName)) {
                deleteFile(dbFileName); // 실제 파일 삭제
                defectImageDAO.deleteImageByFileName(report.getId(), dbFileName); // DB 레코드 삭제
            }
        }

        // 3. 새로운 이미지 파일 저장 및 DB에 이미지 정보 저장
        if (newFiles != null && !newFiles.isEmpty()) {
            for (MultipartFile file : newFiles) {
                try {
                    String savedFileName = saveFile(file);
                    if (savedFileName != null) {
                        DefectImageDTO image = new DefectImageDTO();
                        image.setReportId(report.getId());
                        image.setFileName(savedFileName);
                        defectImageDAO.insertImage(image);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("새 이미지 파일 저장 중 오류 발생", e);
                }
            }
        }
    }

    @Override
    @Transactional
    public void deleteReport(Long id) {
        // 1. 해당 신고에 연결된 모든 이미지 파일 삭제
        List<DefectImageDTO> imagesToDelete = defectImageDAO.selectImagesByReportId(id);
        if (imagesToDelete != null) {
            for (DefectImageDTO image : imagesToDelete) {
                deleteFile(image.getFileName());
            }
        }
        // 2. DB에서 이미지 레코드 삭제 (DEFECT_REPORT 테이블의 FK에 ON DELETE CASCADE가 설정되어 있으므로,
        // DEFECT_REPORT 레코드만 삭제하면 DEFECT_REPORT_IMAGES 레코드는 자동으로 삭제됩니다.)
        // defectImageDAO.deleteImagesByReportId(id); // 이 라인은 필요 없을 수 있습니다.

        // 3. DB에서 신고 레코드 삭제 (DEFECT_REPORT 테이블)
        defectReportDAO.deleteReport(id);
    }

    @Override
    public boolean checkPassword(Long id, String password) {
        String storedPassword = defectReportDAO.selectPasswordById(id);
        return storedPassword != null && storedPassword.equals(password);
    }

    @Override
    @Transactional
    public void updateReportStatus(Long id, String status) {
        DefectReportDTO report = defectReportDAO.selectById(id);
        if (report != null) {
            defectReportDAO.updateStatus(id, status); // 상태만 업데이트

            // 알림 발송
            String username = report.getUsername();
            if (username != null) {
                String title = "결함 신고 처리 상태 변경 알림";
                String message = String.format("회원님의 결함 신고 (ID: %d) 상태가 '%s'(으)로 변경되었습니다.", id, status);
                String link = "/report/detail?id=" + id;

                notificationService.createAndSendNotification(
                    username,
                    "DEFECT_REPORT",
                    title,
                    message,
                    link
                );
            }
        }
    }

    @Override
    public List<RecallSimilarDTO> findSimilarRecalls(String carModel, String defectText, List<RecallDTO> recallList) {
        String searchText = (carModel != null ? carModel : "") + " " + defectText;

        try {
            RestTemplate restTemplate = new RestTemplate();

            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 바디 설정
            Map<String, String> body = new HashMap<>();
            body.put("defect_text", searchText);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            // Flask에 요청 보내기 (List로 받아옴)
            ResponseEntity<List<RecallRecommendDTO>> response = restTemplate.exchange(
                    FLASK_RECOMMEND_URL,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<List<RecallRecommendDTO>>() {}
            );

            List<RecallRecommendDTO> pythonResult = response.getBody();
            if (pythonResult == null) return new ArrayList<>();

            // 화면 출력용 DTO로 변환
            return pythonResult.stream()
                    .map(r -> {
                        RecallDTO dummyRecall = new RecallDTO();
                        dummyRecall.setMaker(r.getMaker());
                        dummyRecall.setModelName(r.getModelName());
                        dummyRecall.setRecallDate(r.getRecallDate());
                        dummyRecall.setRecallReason(r.getRecallReason());

                        return new RecallSimilarDTO(dummyRecall, r.getSimilarity());
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public RecallPredictionDTO getPredictionFromAi(String defectText) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 바디 설정
            Map<String, String> body = new HashMap<>();
            body.put("defect_text", defectText);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            // POST 요청
            return restTemplate.postForObject(FLASK_API_URL, requestEntity, RecallPredictionDTO.class);

        } catch (Exception e) {
            e.printStackTrace();
            return new RecallPredictionDTO();
        }
    }
}
