package com.boot.service;

import com.boot.dao.RecallDAO;
import com.boot.dto.Criteria;
import com.boot.dto.RecallDTO;
import com.boot.dto.RecallStatsFilterDTO;
import com.boot.dto.RecallStatsRowDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecallServiceImpl implements RecallService {

    private final RecallDAO recallDAO;
    private final UserVehicleService userVehicleService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void saveRecallData(List<RecallDTO> recallList) {
        for (RecallDTO recallDTO : recallList) {
            recallDAO.insertRecall(recallDTO);
            checkAndSendRecallNotifications(recallDTO);
        }
    }

    @Override
    @Transactional
    public void insertRecallList(List<RecallDTO> recallList) {
        for (RecallDTO recallDTO : recallList) {
            recallDAO.insertRecall(recallDTO);
            checkAndSendRecallNotifications(recallDTO);
        }
    }

    @Override
    public List<RecallDTO> getAllRecalls(Criteria cri) {
        return recallDAO.selectAll(cri);
    }

    @Override
    public int getRecallCount(Criteria cri) {
        return recallDAO.count(cri);
    }

    @Override
    public List<RecallDTO> searchRecallsByModelName(String modelName) {
        return recallDAO.searchByModelName(modelName);
    }

    @Override
    public int countAllRecalls() {
        return recallDAO.count(new Criteria());
    }

    // -------------------------------------------------------------------
    // 7. 전체 목록 조회 (페이징 없이, CSV 다운로드용)
    // -------------------------------------------------------------------
    @Override
    public List<RecallDTO> getAllRecallsWithoutPaging() {
        return recallDAO.selectAllWithoutPagings();
    }
    @Override
    public List<String> getMakerList() {
        return recallDAO.selectDistinctMaker();
    }

    @Override
    public void checkAndSendRecallNotifications(RecallDTO newRecall) {
        if (newRecall == null || newRecall.getModelName() == null) {
            return;
        }

        List<String> usernames = userVehicleService.getUsernamesByCarModel(newRecall.getModelName());

        for (String username : usernames) {
            String title = "새로운 리콜 정보: " + newRecall.getMaker() + " " + newRecall.getModelName();
            String message = String.format(
                "회원님의 차량 '%s %s'에 대한 새로운 리콜 정보가 등록되었습니다.<br>" +
                "<strong>리콜 사유:</strong> %s<br>" +
                "<strong>리콜 시작일:</strong> %s",
                newRecall.getMaker(), newRecall.getModelName(),
                newRecall.getRecallReason(), newRecall.getRecallDate()
            );
            String link = "/recall-status"; // 리콜 현황 페이지 링크

            notificationService.createAndSendNotification(
                username,
                "RECALL",
                title,
                message,
                link
            );
        }
    }

    @Override
    public RecallDTO getRecallById(Long id) {
        return recallDAO.selectById(id);
    }
    
    @Override
    public List<RecallStatsRowDTO> getRecallStats(RecallStatsFilterDTO filter) {
        // 기본값 세팅 (groupBy/timeUnit/기간 등)
        if (filter.getGroupBy() == null || filter.getGroupBy().isEmpty()) {
            filter.setGroupBy("MANUFACTURER");
        }
        if (filter.getTimeUnit() == null || filter.getTimeUnit().isEmpty()) {
            filter.setTimeUnit("MONTH");
        }
        // startDate/endDate 기본값은 프론트에서 넣어도 되고, 여기서도 세팅 가능 (지금은 생략)

        return recallDAO.selectRecallStats(filter);
    }

    @Override
    public List<RecallDTO> searchByVin(String vin) {
        return recallDAO.searchByVin(vin);
    }

    @Override
    public List<RecallDTO> searchByRegistrationNumber(String registrationNumber) {
        return recallDAO.searchByRegistrationNumber(registrationNumber);
    }
}