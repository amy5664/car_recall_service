package com.boot.dao;

import com.boot.dto.Criteria;
import com.boot.dto.RecallDTO;
import com.boot.dto.RecallStatsFilterDTO;
import com.boot.dto.RecallStatsRowDTO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecallDAO {

    // 단일 리콜 정보 삽입
    void insertRecall(RecallDTO recallDTO);
    
    void insertRecallList(List<RecallDTO> list);

    // 전체 목록 조회 (페이징 및 검색 기능 포함)
    List<RecallDTO> selectAll(Criteria cri);

    // 전체 데이터 수 조회 (검색 기능 포함)
    int count(Criteria cri);

    // 모델명으로 검색
    List<RecallDTO> searchByModelName(@Param("modelName") String modelName);

    // 5. 전체 목록 조회 (페이징 없이, CSV 다운로드용)
    List<RecallDTO> selectAllWithoutPagings();
    
    // 제조사 목록 조회 추가
    List<String> selectDistinctMaker();

    // ID로 리콜 상세 조회
    RecallDTO selectById(Long id);
    
    List<RecallStatsRowDTO> selectRecallStats(RecallStatsFilterDTO filter);

    // VIN으로 검색
    List<RecallDTO> searchByVin(@Param("vin") String vin);

    // 등록번호로 검색
    List<RecallDTO> searchByRegistrationNumber(@Param("registrationNumber") String registrationNumber);
}
