package com.boot.dao;

import com.boot.dto.DefectImageDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DefectImageDAO {
    void insertImage(DefectImageDTO image);
    List<DefectImageDTO> selectImagesByReportId(Long reportId);
    void deleteImagesByReportId(Long reportId);
    void deleteImageByFileName(@Param("reportId") Long reportId, @Param("fileName") String fileName);
}
