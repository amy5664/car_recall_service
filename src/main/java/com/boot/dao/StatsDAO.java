package com.boot.dao;

import com.boot.dto.DailyStatsDTO;
import com.boot.dto.DefectReportDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;

@Mapper
public interface StatsDAO {
    public ArrayList<DailyStatsDTO> getDailyReportStats();
    public ArrayList<DefectReportDTO> getRecentReports();
}