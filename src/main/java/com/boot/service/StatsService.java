package com.boot.service;

import com.boot.dto.DefectReportDTO;
import com.boot.dto.DailyStatsDTO;
import java.util.ArrayList;

public interface StatsService {
    public ArrayList<DailyStatsDTO> getDailyReportStats();
    public ArrayList<DefectReportDTO> getRecentReports();
}