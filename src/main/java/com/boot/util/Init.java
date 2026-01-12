package com.boot.util;

import com.boot.dao.RecallDAO;
import com.boot.dto.Criteria;
import com.boot.dto.RecallDTO;
import com.boot.service.RecallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
// 스프링 빈으로 등록하여 애플리케이션 시작 시 자동으로 실행되도록 합니다. (두 번째 코드의 결정)
@Component
@RequiredArgsConstructor
public class Init implements CommandLineRunner {
    private final CsvParser csvParser;
    private final RecallService recallService;

    @Override
    public void run(String... args) throws Exception {
        // 데이터베이스에 이미 데이터가 있는지 확인
        if (recallService.getRecallCount(new Criteria()) > 0) {
            log.info("데이터베이스에 이미 리콜 데이터가 존재합니다. 초기화 작업을 건너뜁니다.");
            return;
        }

        List<RecallDTO> recallList = csvParser.getRecallList();

        if(recallList.isEmpty()){
            log.info("CSV 파일이 비어있거나 읽을 수 없습니다. 초기화 작업을 건너뜁니다.");
            return;
        }

        // 데이터베이스가 비어있을 때만 데이터 삽입
        recallService.insertRecallList(recallList);

        log.info("CSV 데이터로부터 {}개의 리콜 정보를 데이터베이스에 성공적으로 저장했습니다.", recallList.size());
    }
}