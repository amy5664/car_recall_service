package com.boot.dao;

import com.boot.dto.Criteria;
import com.boot.dto.DefectReportDTO;
import com.boot.dto.RecallDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DefectReportDAO {
    void insertReport(DefectReportDTO report);
    List<DefectReportDTO> selectAll(@Param("cri") Criteria cri, @Param("username") String username); // username 파라미터 추가
    List<DefectReportDTO> selectAllWithoutPaging(); // CSV 다운로드를 위한 전체 목록 조회
    int count(@Param("keyword") String keyword, @Param("username") String username); // username 파라미터 추가
    DefectReportDTO selectById(Long id);
    void updateReport(DefectReportDTO report); // username과 status 필드도 업데이트에 포함
    void updateStatus(@Param("id") Long id, @Param("status") String status);
    void deleteReport(Long id);
    String selectPasswordById(Long id);
    List<RecallDTO> selectAllWithoutPagings();
}
