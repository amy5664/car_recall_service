package com.boot.util;


import com.boot.dto.RecallDTO;
import com.opencsv.CSVReader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Getter
public class CsvParser {

    @Value("classpath:한국교통안전공단_자동차결함 리콜현황_20231231.csv")
    private Resource csvFile;

    private final List<RecallDTO> recallList = new ArrayList<>();

    @PostConstruct
    public void loadCsv() {
        try (CSVReader csvReader = new CSVReader(
                new InputStreamReader(csvFile.getInputStream(), Charset.forName("EUC-KR")))) {

            String[] nextLine;
            csvReader.readNext();
            while ((nextLine = csvReader.readNext()) != null) {


                RecallDTO dto = RecallDTO.builder()
                        .maker(nextLine[0])
                        .modelName(nextLine[1])
                        .makeStart(nextLine[2])
                        .makeEnd(nextLine[3])
                        .recallDate(nextLine[4])
                        .recallReason(nextLine[5])
                        .build();

                recallList.add(dto);
            }

            log.info(" CSV 파일 파싱 완료: ", recallList.size());
        } catch (Exception e) {
            log.error(" CSV 파싱 중 오류 발생", e);
        }
    }
}
