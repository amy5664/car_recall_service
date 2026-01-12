package com.boot.service;

import com.boot.dto.DefectReportDTO;
import com.boot.dto.RecallDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfExportService {

    private static final float PAGE_MARGIN = 50f;
    private static final float TITLE_FONT_SIZE = 18f;
    private static final float SUBTITLE_FONT_SIZE = 12f;
    private static final float BODY_FONT_SIZE = 11f;
    private static final float SECTION_SPACING = 8f;
    private static final DateTimeFormatter HEADER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat REPORT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final ResourceLoader resourceLoader;

    @Value("${app.pdf.font-path:}")
    private String configuredFontPath;

    /**
     * 리콜 데이터를 기반으로 PDF 파일을 생성합니다.
     */
    public byte[] generateRecallPdf(List<RecallDTO> recallList) throws IOException {
        List<RecallDTO> data = recallList == null ? Collections.emptyList() : recallList;
        try (PDDocument document = new PDDocument();
             PdfPageState pageState = new PdfPageState(document);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDFont font = resolveFont(document);

            writeTitle(pageState, font, "자동차 리콜 내역 보고서", data.size());

            if (data.isEmpty()) {
                writeParagraph(pageState, font, "PDF로 내보낼 리콜 데이터가 없습니다.", BODY_FONT_SIZE);
            } else {
                int index = 1;
                for (RecallDTO recall : data) {
                    writeParagraph(pageState, font, index + ". 제조사: " + safeText(recall.getMaker()), BODY_FONT_SIZE);
                    writeParagraph(pageState, font, "   모델명: " + safeText(recall.getModelName()), BODY_FONT_SIZE);
                    writeParagraph(pageState, font, "   생산 기간: " + buildDateRange(recall.getMakeStart(), recall.getMakeEnd()), BODY_FONT_SIZE);
                    writeParagraph(pageState, font, "   리콜 일자: " + safeText(recall.getRecallDate()), BODY_FONT_SIZE);
                    // buildFieldLines 호출 시 font와 fontSize 전달
                    writeParagraph(pageState, font, buildFieldLines(font, BODY_FONT_SIZE, "   리콜 사유: ", recall.getRecallReason()), BODY_FONT_SIZE);
                    addSectionSpacing(pageState);
                    index++;
                }
            }

            pageState.finish();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * 결함 신고 데이터를 기반으로 PDF 파일을 생성합니다.
     */
    public byte[] generateDefectReportPdf(List<DefectReportDTO> reportList) throws IOException {
        List<DefectReportDTO> data = reportList == null ? Collections.emptyList() : reportList;
        try (PDDocument document = new PDDocument();
             PdfPageState pageState = new PdfPageState(document);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDFont font = resolveFont(document);

            writeTitle(pageState, font, "결함 신고 내역 보고서", data.size());

            if (data.isEmpty()) {
                writeParagraph(pageState, font, "PDF로 내보낼 신고 내역이 없습니다.", BODY_FONT_SIZE);
            } else {
                int index = 1;
                for (DefectReportDTO report : data) {
                    writeParagraph(pageState, font, index + ". 신고번호: " + safeText(report.getId()), BODY_FONT_SIZE);
                    writeParagraph(pageState, font, "   신고인: " + safeText(report.getReporterName()), BODY_FONT_SIZE);
                    writeParagraph(pageState, font, "   연락처: " + safeText(report.getContact()), BODY_FONT_SIZE);
                    writeParagraph(pageState, font, "   차량 모델: " + safeText(report.getCarModel()), BODY_FONT_SIZE);
                    writeParagraph(pageState, font, "   차대번호: " + safeText(report.getVin()), BODY_FONT_SIZE);
                    // buildFieldLines 호출 시 font와 fontSize 전달
                    writeParagraph(pageState, font, buildFieldLines(font, BODY_FONT_SIZE, "   결함 내용: ", report.getDefectDetails()), BODY_FONT_SIZE);
                    writeParagraph(pageState, font, "   신고일시: " + formatReportDate(report), BODY_FONT_SIZE);
                    addSectionSpacing(pageState);
                    index++;
                }
            }

            pageState.finish();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private void writeTitle(PdfPageState pageState, PDFont font, String title, int totalCount) throws IOException {
        String generatedAt = HEADER_DATE_FORMAT.format(LocalDateTime.now());
        writeParagraph(pageState, font, title, TITLE_FONT_SIZE);
        writeParagraph(pageState, font, "총 " + totalCount + "건 · 생성일시 " + generatedAt, SUBTITLE_FONT_SIZE);
        addSectionSpacing(pageState);
    }

    private void writeParagraph(PdfPageState pageState, PDFont font, String text, float fontSize) throws IOException {
        writeParagraph(pageState, font, Collections.singletonList(text), fontSize);
    }

    private void writeParagraph(PdfPageState pageState, PDFont font, List<String> lines, float fontSize) throws IOException {
        List<String> safeLines = lines == null ? Collections.singletonList("") : lines;
        for (String line : safeLines) {
            writeLine(pageState, font, fontSize, line);
        }
    }

    private void writeLine(PdfPageState pageState, PDFont font, float fontSize, String text) throws IOException {
        float lineHeight = fontSize + 4f;
        pageState.ensureSpace(lineHeight);
        PDPageContentStream contentStream = pageState.getContentStream();
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(PAGE_MARGIN, pageState.getCursorY());
        if (StringUtils.hasLength(text)) {
            contentStream.showText(sanitizeForPdf(text));
        }
        contentStream.endText();
        pageState.moveCursor(lineHeight);
    }

    /**
     * 폰트가 지원하지 않는 제어문자(특히 탭)를 안전한 문자로 치환합니다.
     */
    private String sanitizeForPdf(String text) {
        if (text == null) {
            return "";
        }
        // 탭을 스페이스 4개로 대체
        String replaced = text.replace("\t", "    ");
        // CR은 이미 제거되어 있음; 혹시 남아있는 다른 제어문자는 제거
        StringBuilder sb = new StringBuilder(replaced.length());
        for (int i = 0; i < replaced.length(); i++) {
            char c = replaced.charAt(i);
            if (c >= 0x20 || c == '\n') {
                sb.append(c);
            } else {
                // 제어문자는 공백으로 대체
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private void addSectionSpacing(PdfPageState pageState) throws IOException {
        pageState.ensureSpace(SECTION_SPACING);
        pageState.moveCursor(SECTION_SPACING);
    }

    private String buildDateRange(String start, String end) {
        String safeStart = StringUtils.hasText(start) ? start : "-";
        String safeEnd = StringUtils.hasText(end) ? end : "-";
        return safeStart + " ~ " + safeEnd;
    }

    // 기존 splitByLength 메서드를 대체하고, font와 fontSize를 인자로 받도록 변경
    private List<String> buildFieldLines(PDFont font, float fontSize, String label, String value) throws IOException {
        String safeValue = safeText(value); // safeText returns "-" for null/empty
        String sanitizedValue = sanitizeForPdf(safeValue); // 여기서 탭 문자를 제거합니다.

        float totalAvailableWidth = PDRectangle.A4.getWidth() - 2 * PAGE_MARGIN;
        float labelWidth = font.getStringWidth(label) / 1000 * fontSize;
        float availableWidthForValue = totalAvailableWidth - labelWidth;

        // 레이블이 너무 길어 값 영역이 음수가 되거나 너무 작아지는 경우, 값을 다음 줄에 전체 너비로 표시
        if (availableWidthForValue < 50) { // 최소한의 합리적인 값 너비
            availableWidthForValue = totalAvailableWidth;
            labelWidth = 0; // 레이블은 사실상 자체 줄을 차지
        }

        List<String> valueLines = splitTextToFitWidth(sanitizedValue, font, fontSize, availableWidthForValue); // sanitize된 값을 사용
        List<String> result = new ArrayList<>();

        if (valueLines.isEmpty() || (valueLines.size() == 1 && valueLines.get(0).equals("-"))) {
            result.add(label + "-");
            return result;
        }

        // 첫 번째 줄: 레이블 + 값의 첫 부분
        result.add(label + valueLines.get(0));

        // 후속 줄: 들여쓰기 + 값의 나머지 부분
        StringBuilder indentBuilder = new StringBuilder();
        float currentIndentWidth = 0;
        float spaceWidth = font.getStringWidth(" ") / 1000 * fontSize;

        // 레이블 너비만큼 들여쓰기 공간 계산
        while (currentIndentWidth < labelWidth && spaceWidth > 0) {
            indentBuilder.append(" ");
            currentIndentWidth += spaceWidth;
        }
        String indent = indentBuilder.toString();

        // 레이블 너비가 0인 경우 (레이블이 자체 줄을 차지하는 경우) 기본 들여쓰기
        if (labelWidth == 0) {
            indent = "    "; // 4칸 공백 기본 들여쓰기
        }

        for (int i = 1; i < valueLines.size(); i++) {
            result.add(indent + valueLines.get(i));
        }
        return result;
    }

    // 텍스트를 실제 너비에 맞춰 여러 줄로 분할하는 새로운 메서드
    private List<String> splitTextToFitWidth(String text, PDFont font, float fontSize, float availableWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (!StringUtils.hasText(text) || text.equals("-")) {
            return Collections.singletonList(text);
        }

        String[] paragraphs = text.replace("\r", "").split("\n"); // 명시적 줄 바꿈 먼저 처리

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            StringBuilder currentLine = new StringBuilder();
            float currentLineWidth = 0;

            // 공백을 기준으로 토큰화 (공백 자체도 토큰으로 유지)
            String[] words = paragraph.split("(?<=\\s)|(?=\\s)");
            if (words.length == 0 && !paragraph.isEmpty()) { // 공백 없는 긴 단어 처리
                words = new String[]{paragraph};
            } else if (words.length == 0 && paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            for (String word : words) {
                float wordWidth = font.getStringWidth(word) / 1000 * fontSize;

                if (currentLineWidth + wordWidth <= availableWidth) {
                    currentLine.append(word);
                    currentLineWidth += wordWidth;
                } else {
                    // 단어가 현재 줄에 들어가지 않음
                    if (currentLine.length() > 0) { // 현재 줄에 내용이 있으면 추가
                        lines.add(currentLine.toString().trim()); // 후행 공백 제거
                    }
                    currentLine = new StringBuilder(word);
                    currentLineWidth = wordWidth;

                    // 단어 자체가 너무 긴 경우, 글자 단위로 분할
                    if (currentLineWidth > availableWidth && word.length() > 1) {
                        List<String> subWordLines = splitLongWord(word, font, fontSize, availableWidth);
                        for (int i = 0; i < subWordLines.size(); i++) {
                            if (i == 0) { // 긴 단어의 첫 부분은 현재 줄이 됨
                                currentLine = new StringBuilder(subWordLines.get(i));
                                currentLineWidth = font.getStringWidth(subWordLines.get(i)) / 1000 * fontSize;
                            } else { // 나머지 부분은 새 줄로
                                lines.add(currentLine.toString().trim());
                                currentLine = new StringBuilder(subWordLines.get(i));
                                currentLineWidth = font.getStringWidth(subWordLines.get(i)) / 1000 * fontSize;
                            }
                        }
                    }
                }
            }
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString().trim());
            }
        }
        return lines;
    }

    // 단일 긴 단어를 글자 단위로 분할하는 헬퍼 메서드
    private List<String> splitLongWord(String word, PDFont font, float fontSize, float availableWidth) throws IOException {
        List<String> subWordLines = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();
        float currentPartWidth = 0;

        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            String charStr = String.valueOf(c);
            float charWidth = font.getStringWidth(charStr) / 1000 * fontSize;

            if (currentPartWidth + charWidth <= availableWidth) {
                currentPart.append(c);
                currentPartWidth += charWidth;
            } else {
                subWordLines.add(currentPart.toString());
                currentPart = new StringBuilder(charStr);
                currentPartWidth = charWidth;
            }
        }
        if (currentPart.length() > 0) {
            subWordLines.add(currentPart.toString());
        }
        return subWordLines;
    }

    private String repeatSpace(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(' ');
        }
        return builder.toString();
    }

    private String safeText(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? "-" : text.trim();
    }

    private String formatReportDate(DefectReportDTO report) {
        Object dateObject = report != null ? report.getReportDate() : null;
        if (dateObject instanceof java.util.Date) {
            return REPORT_DATE_FORMAT.format((java.util.Date) dateObject);
        }
        return "-";
    }

    private PDFont resolveFont(PDDocument document) throws IOException {
        List<String> candidates = new ArrayList<>();
        if (StringUtils.hasText(configuredFontPath)) {
            candidates.add(configuredFontPath);
        }
        candidates.add("classpath:fonts/NanumGothic.ttf");
        candidates.add("C:/Windows/Fonts/malgun.ttf");
        candidates.add("/usr/share/fonts/truetype/nanum/NanumGothic.ttf");

        for (String candidate : candidates) {
            PDFont font = tryLoadFont(document, candidate);
            if (font != null) {
                log.debug("Loaded PDF font from {}", candidate);
                return font;
            }
        }

        log.warn("한글을 지원하는 폰트를 찾지 못해 기본 서체(Helvetica)로 대체합니다. " +
                "필요 시 app.pdf.font-path 속성으로 폰트를 지정해 주세요.");
        return PDType1Font.HELVETICA;
    }

    private PDFont tryLoadFont(PDDocument document, String location) {
        if (!StringUtils.hasText(location)) {
            return null;
        }
        try {
            if (location.startsWith("classpath:")) {
                Resource resource = resourceLoader.getResource(location);
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        return PDType0Font.load(document, is);
                    }
                }
            } else {
                Path path = Paths.get(location);
                if (Files.exists(path)) {
                    try (InputStream is = Files.newInputStream(path)) {
                        return PDType0Font.load(document, is);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to load font from {}", location, e);
        }
        return null;
    }

    /**
     * 페이지 상태를 관리하는 헬퍼 클래스입니다.
     */
    private static class PdfPageState implements Closeable {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float cursorY;

        PdfPageState(PDDocument document) {
            this.document = Objects.requireNonNull(document, "document");
        }

        void ensureSpace(float requiredHeight) throws IOException {
            if (page == null) {
                startNewPage();
                return;
            }
            if (cursorY - requiredHeight < PAGE_MARGIN) {
                startNewPage();
            }
        }

        PDPageContentStream getContentStream() {
            return contentStream;
        }

        float getCursorY() {
            return cursorY;
        }

        void moveCursor(float height) {
            cursorY -= height;
        }

        void finish() throws IOException {
            closeContentStream();
        }

        private void startNewPage() throws IOException {
            closeContentStream();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            cursorY = page.getMediaBox().getHeight() - PAGE_MARGIN;
        }

        private void closeContentStream() throws IOException {
            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }

        @Override
        public void close() throws IOException {
            closeContentStream();
        }
    }
}