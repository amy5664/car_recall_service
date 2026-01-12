package com.boot.service;

import com.boot.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DefectReportService {
    void saveReport(DefectReportDTO report, List<MultipartFile> files);
    List<DefectReportDTO> getAllReports(Criteria cri, String username); // username 파라미터 추가
    List<DefectReportDTO> getAllReportsWithoutPaging(); // CSV 다운로드를 위한 전체 목록 조회
    int getTotalCount(Criteria cri, String username); // username 파라미터 추가
    DefectReportDTO getReportById(Long id);
    void updateReport(DefectReportDTO report, List<MultipartFile> newFiles, List<String> existingFileNames);
    void deleteReport(Long id);
    boolean checkPassword(Long id, String password);
    void updateReportStatus(Long id, String status); // 결함 신고 상태 업데이트 메서드 추가
    List<RecallSimilarDTO> findSimilarRecalls(String carModel,
                                              String defectText,
                                              List<RecallDTO> recallList);
    public RecallPredictionDTO getPredictionFromAi(String defectText);
}