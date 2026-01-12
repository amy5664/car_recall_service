package com.boot.controller;

import com.boot.dto.Criteria;
import com.boot.dto.NoticeDTO;
import com.boot.dto.BoardDTO;
import com.boot.dto.DefectReportDTO;
import com.boot.dto.PageDTO;
import com.boot.dto.RecallDTO;
import com.boot.dto.SearchResultsDTO;
import com.boot.service.DefectReportService;
import com.boot.service.NoticeService;
import com.boot.service.BoardService;
import com.boot.service.PdfExportService;
import com.boot.service.RecallService;
import com.boot.dto.*;
import com.boot.service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.security.Principal;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse; // 이 import 문을 추가합니다.
import com.opencsv.CSVWriter;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {

    private final ResourceLoader resourceLoader;
    private final RecallService recallService;
    private final DefectReportService defectReportService;
    private final NoticeService noticeService;
    private final BoardService boardService;
    private final PdfExportService pdfExportService;
    private final SearchService searchService;

    // -------------------------------------------------------------------
    // 1. 메인 페이지 (리콜 검색 기능 포함)
    // -------------------------------------------------------------------
    @GetMapping("/")
    public String main(Model model,
                       @RequestParam(value = "searchType", required = false, defaultValue = "vin") String searchType, // 기본값을 'vin'으로 변경
                       @RequestParam(value = "query", required = false) String query) {
        // 최근 공지사항 5개 조회
        Criteria noticeCri = new Criteria(1, 5);
        List<NoticeDTO> noticeList = noticeService.listWithPaging(noticeCri);
        model.addAttribute("noticeList", noticeList);

        // 최근 보도자료 5개 조회
        Criteria pressCri = new Criteria(1, 5);
        List<BoardDTO> pressList = boardService.listWithPaging(pressCri);
        model.addAttribute("pressList", pressList);

        // 메인 페이지 리콜 검색 처리
        if (query != null && !query.trim().isEmpty()) {
            List<RecallDTO> recallList = null;
            switch (searchType) {
                case "vin":
                    recallList = recallService.searchByVin(query.trim());
                    break;
                case "regNum":
                    recallList = recallService.searchByRegistrationNumber(query.trim());
                    break;
                // modelName 검색 로직은 제거
                default: // 기본값은 vin으로 처리
                    recallList = recallService.searchByVin(query.trim());
                    break;
            }
            model.addAttribute("searchType", searchType); // 선택된 검색 타입 유지
            model.addAttribute("query", query); // 검색어 유지
            model.addAttribute("recallList", recallList); // 검색 결과
        }

        return "main";
    }

    // -------------------------------------------------------------------
    // 통합 검색 처리
    // -------------------------------------------------------------------
    @GetMapping("/search")
    public String search(@RequestParam(value = "query") String query,
                         @RequestParam(value = "category", required = false, defaultValue = "all") String category,
                         Model model) {
        log.info("@# Search query: {}, category: {}", query, category);
        SearchResultsDTO results = searchService.searchAll(query, category);
        model.addAttribute("results", results);
        model.addAttribute("selectedCategory", category); // 현재 선택된 카테고리를 뷰로 전달
        return "search_results";
    }

    // -------------------------------------------------------------------
    // 2. 리콜 현황 페이지 (페이징 기능 포함)
    // URL: /recall-status
    // -------------------------------------------------------------------
    @GetMapping("/recall-status")
    public String recallStatus(Criteria cri, Model model) {
        List<RecallDTO> recallList = recallService.getAllRecalls(cri);
        model.addAttribute("recallList", recallList);

        int total = recallService.getRecallCount(cri);

        PageDTO pageDTO = new PageDTO(cri, total);
        model.addAttribute("pageMaker", pageDTO);

        if (total == 0) {
            model.addAttribute("errorMessage", "데이터베이스에 리콜 데이터가 없습니다. 먼저 데이터를 로드해주세요.");
        }
        return "recall_status";
    }

    // -------------------------------------------------------------------
    // 3. 데이터 로드 및 초기화 기능
    // -------------------------------------------------------------------
    @GetMapping("/load-data")
    public String loadData(Model model) {
        ObjectMapper objectMapper = new ObjectMapper();
        Random random = new Random();
        try {
            Resource resource = resourceLoader.getResource("classpath:integrated_recall_data.json");
            InputStream inputStream = resource.getInputStream();
            List<RecallDTO> recallList = objectMapper.readValue(inputStream, new TypeReference<List<RecallDTO>>() {});

            recallList.forEach(recall -> {
                recall.setVin(generateRandomVin(random));
                recall.setRegistrationNumber(generateRandomRegistrationNumber(random));
            });

            recallService.saveRecallData(recallList);

            int count = recallService.getRecallCount(new Criteria());
            model.addAttribute("message", "성공적으로 " + count + "개의 리콜 데이터를 데이터베이스에 저장했습니다.");

        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "오류가 발생하여 데이터 로드 및 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "load_result";
    }

    // -------------------------------------------------------------------
    // 4. 결함 신고 접수 (폼)
    // URL: /report/write
    // -------------------------------------------------------------------
    @GetMapping("/report/write")
    public String defectReportForm() {
        return "defect_report";
    }

    // -------------------------------------------------------------------
    // 5. 결함 신고 접수 처리
    // URL: /defect-report (POST)
    // -------------------------------------------------------------------
    @PostMapping("/defect-report")
    public String defectReportSubmit(DefectReportDTO report, @RequestParam(value = "defectImages", required = false) List<MultipartFile> files, RedirectAttributes rttr, Principal principal) {
        try {
            if (principal != null) {
                report.setUsername(principal.getName());
            }
            report.setStatus("접수"); // 상태를 '접수'로 초기화
            defectReportService.saveReport(report, files);
            rttr.addFlashAttribute("message", "결함 신고가 성공적으로 접수되었습니다. 신고 내역에서 확인해 주세요.");
        } catch (Exception e) {
            e.printStackTrace();
            rttr.addFlashAttribute("errorMessage", "오류가 발생하여 신고가 접수되지 않았습니다: " + e.getMessage());
        }
        return "redirect:/report/history";
    }

    // -------------------------------------------------------------------
    // 6. 결함 신고 목록 조회 (페이징 기능 포함)
    // URL: /report/history
    // -------------------------------------------------------------------
    @GetMapping("/report/history")
    public String defectReportList(Criteria cri, Model model, Principal principal, HttpServletResponse response) { // HttpServletResponse 추가
        log.info("@# Get report history for user: {}", principal != null ? principal.getName() : "anonymous");

        // 캐시 제어 헤더 추가
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0
        response.setHeader("Expires", "0"); // Proxies

        String username = null;
        if (principal != null) {
            username = principal.getName();
        }

        List<DefectReportDTO> reportList = defectReportService.getAllReports(cri, username); // username 전달
        model.addAttribute("reportList", reportList);

        int total = defectReportService.getTotalCount(cri, username); // getTotalCount에도 username 전달
        PageDTO pageDTO = new PageDTO(cri, total);
        model.addAttribute("pageMaker", pageDTO);

        return "defect_report_list";
    }

    // -------------------------------------------------------------------
    // 7. 결함 신고 상세 조회
    // URL: /report/detail
    // -------------------------------------------------------------------
    @GetMapping("/report/detail")
    public String defectReportDetail(@RequestParam("id") Long id, Model model) {
        DefectReportDTO report = defectReportService.getReportById(id);
        if (report != null) {
            // 2. AI(Python) 서버에 예측 요청 (결함 내용 전달)
            RecallPredictionDTO prediction = defectReportService.getPredictionFromAi(report.getDefectDetails());

            // 3. 모델에 담기 (JSP에서 recallPrediction 으로 사용 중)
            model.addAttribute("recallPrediction", prediction);
        }
        model.addAttribute("report", report);
        return "defect_report_detail";
    }

    // -------------------------------------------------------------------
    // 8. 결함 신고 수정 폼 요청 (비밀번호 검증 포함)
    // URL: /report/edit
    // -------------------------------------------------------------------
    @GetMapping("/report/edit")
    public String defectReportEditForm(@RequestParam("id") Long id, @RequestParam("password") String password, Model model, RedirectAttributes rttr) {
        if (!defectReportService.checkPassword(id, password)) {
            rttr.addFlashAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            return "redirect:/report/detail?id=" + id;
        }
        DefectReportDTO report = defectReportService.getReportById(id);
        model.addAttribute("report", report);
        return "defect_report_edit";
    }

    // -------------------------------------------------------------------
    // 9. 결함 신고 수정 처리
    // URL: /report/edit (POST)
    // -------------------------------------------------------------------
    @PostMapping("/report/edit")
    public String defectReportEditSubmit(DefectReportDTO report,
                                         @RequestParam(value = "newDefectImages", required = false) List<MultipartFile> newFiles,
                                         @RequestParam(value = "existingImageFileNames", required = false) List<String> existingFileNames,
                                         RedirectAttributes rttr) {
        try {
            defectReportService.updateReport(report, newFiles, existingFileNames);
            rttr.addFlashAttribute("message", "결함 신고가 성공적으로 수정되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            rttr.addFlashAttribute("errorMessage", "오류가 발생하여 신고 수정에 실패했습니다: " + e.getMessage());
        }
        return "redirect:/report/detail?id=" + report.getId();
    }

    // -------------------------------------------------------------------
    // 10. 결함 신고 삭제 처리
    // URL: /report/delete (POST)
    // -------------------------------------------------------------------
    @PostMapping("/report/delete")
    public String defectReportDelete(@RequestParam("id") Long id, @RequestParam("password") String password, RedirectAttributes rttr) {
        try {
            if (!defectReportService.checkPassword(id, password)) {
                rttr.addFlashAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
                return "redirect:/report/detail?id=" + id;
            }
            defectReportService.deleteReport(id);
            rttr.addFlashAttribute("message", "결함 신고가 성공적으로 삭제되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            rttr.addFlashAttribute("errorMessage", "오류가 발생하여 신고 삭제에 실패했습니다: " + e.getMessage());
        }
        return "redirect:/report/history";
    }

    // -------------------------------------------------------------------
    // 11. 리콜 데이터 CSV 다운로드
    // URL: /recall/download/csv
    // -------------------------------------------------------------------
    @GetMapping("/recall/download/csv")
    public void downloadRecallCsv(HttpServletResponse response) throws IOException {
        log.info("@# Recall CSV download requested");

        // 전체 리콜 데이터 조회
        List<RecallDTO> recallList = recallService.getAllRecallsWithoutPaging();

        // CSV 파일명 설정 (한글 파일명 처리)
        String fileName = "리콜내역_" + System.currentTimeMillis() + ".csv";
        String encodedFileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

        // 응답 헤더 설정
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");
        response.setCharacterEncoding("UTF-8");

        // BOM 추가 (Excel에서 한글 깨짐 방지) - Writer 생성 전에 먼저 작성
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);

        // CSV 작성
        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
             CSVWriter csvWriter = new CSVWriter(writer)) {

            // CSV 헤더 작성
            String[] header = {"번호", "제조사", "모델명", "제조시작일", "제조종료일", "리콜일자", "리콜사유"};
            csvWriter.writeNext(header);

            // 데이터 작성
            for (RecallDTO recall : recallList) {
                String[] row = {
                    recall.getId() != null ? String.valueOf(recall.getId()) : "",
                    recall.getMaker() != null ? recall.getMaker() : "",
                    recall.getModelName() != null ? recall.getModelName() : "",
                    recall.getMakeStart() != null ? recall.getMakeStart() : "",
                    recall.getMakeEnd() != null ? recall.getMakeEnd() : "",
                    recall.getRecallDate() != null ? recall.getRecallDate() : "",
                    recall.getRecallReason() != null ? recall.getRecallReason().replace("\n", " ").replace("\r", "") : ""
                };
                csvWriter.writeNext(row);
            }

            csvWriter.flush();
        } catch (Exception e) {
            log.error("@# Recall CSV download error", e);
            throw new IOException("CSV 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    // -------------------------------------------------------------------
    // 12. 리콜 데이터 PDF 다운로드
    // URL: /recall/download/pdf
    // -------------------------------------------------------------------
    @GetMapping("/recall/download/pdf")
    public void downloadRecallPdf(HttpServletResponse response) throws IOException {
        log.info("@# Recall PDF download requested");

        List<RecallDTO> recallList = recallService.getAllRecallsWithoutPaging();
        byte[] pdfBytes = pdfExportService.generateRecallPdf(recallList);
        writePdfResponse(response, pdfBytes, "리콜내역");
    }

    // -------------------------------------------------------------------
    // 13. 결함 신고(리콜 신청) 내역 CSV 다운로드
    // URL: /report/download/csv
    // -------------------------------------------------------------------
    @GetMapping("/report/download/csv")
    public void downloadDefectReportCsv(HttpServletResponse response) throws IOException {
        log.info("@# Defect Report CSV download requested");

        // 전체 결함 신고 데이터 조회
        List<DefectReportDTO> reportList = defectReportService.getAllReportsWithoutPaging();

        // CSV 파일명 설정 (한글 파일명 처리)
        String fileName = "리콜신청내역_" + System.currentTimeMillis() + ".csv";
        String encodedFileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

        // 응답 헤더 설정
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");
        response.setCharacterEncoding("UTF-8");

        // BOM 추가 (Excel에서 한글 깨짐 방지) - Writer 생성 전에 먼저 작성
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);

        // CSV 작성
        try (Writer writer = new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8);
             CSVWriter csvWriter = new CSVWriter(writer)) {

            // CSV 헤더 작성
            String[] header = {"번호", "신고자명", "연락처", "차량모델", "VIN", "결함내용", "신고일자"};
            csvWriter.writeNext(header);

            // 데이터 작성
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (DefectReportDTO report : reportList) {
                String reportDateStr = "";
                if (report.getReportDate() != null) {
                    reportDateStr = dateFormat.format(report.getReportDate());
                }

                String[] row = {
                    report.getId() != null ? String.valueOf(report.getId()) : "",
                    report.getReporterName() != null ? report.getReporterName() : "",
                    report.getContact() != null ? report.getContact() : "",
                    report.getCarModel() != null ? report.getCarModel() : "",
                    report.getVin() != null ? report.getVin() : "",
                    report.getDefectDetails() != null ? report.getDefectDetails().replace("\n", " ").replace("\r", "") : "",
                    reportDateStr
                };
                csvWriter.writeNext(row);
            }

            csvWriter.flush();
        } catch (Exception e) {
            log.error("@# Defect Report CSV download error", e);
            throw new IOException("CSV 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    // -------------------------------------------------------------------
    // 14. 결함 신고(리콜 신청) 내역 PDF 다운로드
    // URL: /report/download/pdf
    // -------------------------------------------------------------------
    @GetMapping("/report/download/pdf")
    public void downloadDefectReportPdf(HttpServletResponse response) throws IOException {
        log.info("@# Defect Report PDF download requested");

        List<DefectReportDTO> reportList = defectReportService.getAllReportsWithoutPaging();
        byte[] pdfBytes = pdfExportService.generateDefectReportPdf(reportList);
        writePdfResponse(response, pdfBytes, "리콜신청내역");
    }

    private void writePdfResponse(HttpServletResponse response, byte[] pdfBytes, String prefix) throws IOException {
        byte[] payload = pdfBytes == null ? new byte[0] : pdfBytes;
        String fileName = prefix + "_" + System.currentTimeMillis() + ".pdf";
        String encodedFileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"");
        response.setContentLength(payload.length);
        response.getOutputStream().write(payload);
    }
    // 11. 리콜 상세 조회
    // URL: /recall/detail/{id}
    // -------------------------------------------------------------------
    @GetMapping("/recall/detail/{id}")
    public String recallDetail(@PathVariable("id") Long id, Model model) {
        RecallDTO recall = recallService.getRecallById(id);
        model.addAttribute("recall", recall);
        return "recall_detail";
    }

    // -------------------------------------------------------------------
    // 랜덤 VIN 생성 유틸리티 메서드
    // -------------------------------------------------------------------
    private String generateRandomVin(Random random) {
        String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder vin = new StringBuilder(17);
        for (int i = 0; i < 17; i++) {
            vin.append(characters.charAt(random.nextInt(characters.length())));
        }
        return vin.toString();
    }

    // -------------------------------------------------------------------
    // 랜덤 자동차 등록번호 생성 유틸리티 메서드 (예: 12가3456)
    // -------------------------------------------------------------------
    private String generateRandomRegistrationNumber(Random random) {
        StringBuilder regNum = new StringBuilder();
        // 앞 두 자리 숫자
        regNum.append(random.nextInt(90) + 10); // 10-99
        // 한글 문자
        String[] hangul = {"가", "나", "다", "라", "마", "거", "너", "더", "러", "머", "고", "노", "도", "로", "모", "구", "누", "두", "루", "무", "바", "사", "아", "자", "차", "카", "타", "파", "하"};
        regNum.append(hangul[random.nextInt(hangul.length)]);
        // 뒤 네 자리 숫자
        regNum.append(String.format("%04d", random.nextInt(10000))); // 0000-9999
        return regNum.toString();
    }//

    @GetMapping("/report/similar-recalls")
    public String getSimilarRecalls2(@RequestParam("id") Long id, Model model) {
        DefectReportDTO report = defectReportService.getReportById(id);
        if(report == null) {
            model.addAttribute("errorMessage", "정보를 찾을 수 없습니다.");
            return "defect_report_detail";
        }

        List<RecallDTO> recallList = recallService.getAllRecallsWithoutPaging();
        List<RecallSimilarDTO> similarRecalls = defectReportService.findSimilarRecalls(
                report.getCarModel(),
                report.getDefectDetails(),
                recallList
        );

        model.addAttribute("report", report);
        model.addAttribute("similarRecalls", similarRecalls);

        return "recall_similar_status";
    }

    // ===================================================================
    // 테스트용: K7 리콜 알림 발송
    // ===================================================================
    @GetMapping("/test/send-k7-recall")
    public String testSendK7Recall(RedirectAttributes rttr) {
        log.info("@# 테스트: K7 리콜 알림 발송 시작");

        // 1. 가상의 리콜 정보 생성
        RecallDTO k7Recall = new RecallDTO();
        k7Recall.setMaker("현대자동차");
        k7Recall.setModelName("k7"); // 모델명은 DB에 저장된 값과 정확히 일치해야 합니다.
        k7Recall.setRecallDate("2024-05-21");
        k7Recall.setRecallReason("엔진 소프트웨어 결함으로 인한 주행 중 시동 꺼짐 가능성 (테스트 데이터)");
        k7Recall.setMakeStart("2022-01-01");
        k7Recall.setMakeEnd("2022-12-31");

        // 2. 알림 발송 로직 호출
        // 이 메서드는 내부적으로 DB에 리콜 정보를 저장하고, 알림을 발송합니다.
        recallService.checkAndSendRecallNotifications(k7Recall);

        log.info("@# 테스트: K7 리콜 알림 발송 완료");
        rttr.addFlashAttribute("message", "K7 리콜 알림 테스트가 실행되었습니다. '내 알림'과 이메일을 확인해주세요.");
        return "redirect:/";
    }
}
