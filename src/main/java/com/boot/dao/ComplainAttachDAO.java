package com.boot.dao;

import com.boot.dto.ComplainAttachDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ComplainAttachDAO {
    public void insert(ComplainAttachDTO dto);
    public void delete(String uuid); // uuid로 삭제하는 메서드 추가
    public List<ComplainAttachDTO> findByReportId(int report_id);
    public void deleteAll(int report_id);
    public ComplainAttachDTO findByUuid(String uuid); // 추가
}
